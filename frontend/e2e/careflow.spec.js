import { test, expect } from "@playwright/test";

async function login(page, role = "Administrator") {
  await page.goto("/");
  await page.getByRole("button", { name: role, exact: true }).click();
  await page.getByRole("button", { name: "Sign in to workspace" }).click();
  await expect(
    page.getByRole("heading", { name: "A clearer view. Better flow." }),
  ).toBeVisible();
  await expect(
    page.getByText("Connecting your operations workspace…"),
  ).toHaveCount(0);
}
async function openRequests(page) {
  await page
    .getByRole("navigation")
    .getByRole("button", { name: /Supply requests/ })
    .click();
}

test("login and dashboard render with no browser errors", async ({
  page,
}, testInfo) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await page.screenshot({
    path: testInfo.outputPath("login.png"),
    fullPage: true,
  });
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Recent supply requests" }),
  ).toBeVisible();
  await expect(page.locator(".cf-table tbody tr")).not.toHaveCount(0);
  await page.screenshot({
    path: testInfo.outputPath("overview.png"),
    fullPage: true,
  });
  expect(errors).toEqual([]);
});

test("inventory search, quality holds, and CSV export work", async ({
  page,
}, testInfo) => {
  await login(page, "Inventory manager");
  await page
    .getByRole("navigation")
    .getByRole("button", { name: "Inventory", exact: true })
    .click();
  await page.getByLabel("Search inventory").fill("SYN-UDI-10001");
  await expect(page.locator(".cf-table tbody tr")).toHaveCount(1);
  await expect(
    page.getByText("Cardiac pacing lead kit", { exact: true }),
  ).toBeVisible();
  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export", exact: true }).click();
  expect((await download).suggestedFilename()).toBe("careflow-inventory.csv");
  await page.getByLabel("Search inventory").fill("");
  await page.getByRole("button", { name: "Quarantined", exact: true }).click();
  await expect(
    page.getByText("Pump reservoir cartridge", { exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("inventory.png"),
    fullPage: true,
  });
});

test("requester can submit a request and see its persistent history", async ({
  page,
}) => {
  await login(page, "Requester");
  await page.getByRole("button", { name: "New request", exact: true }).click();
  const dialog = page.getByRole("dialog");
  await dialog
    .getByLabel("Supply resource")
    .selectOption({
      label: await dialog
        .getByLabel("Supply resource")
        .locator("option")
        .filter({ hasText: "Cardiac pacing lead kit" })
        .textContent(),
    });
  await dialog.getByLabel("Quantity").fill("1");
  await dialog
    .getByLabel("Purpose")
    .fill("Browser verified synthetic replenishment");
  const submitted = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/careflow/requests") &&
      r.request().method() === "POST",
  );
  await dialog.getByRole("button", { name: "Submit request" }).click();
  const created = (await (await submitted).json()).data;
  await expect(page.getByRole("status")).toContainText("submitted");
  await openRequests(page);
  await page
    .getByRole("button", {
      name: `CF-${String(created.id).padStart(4, "0")}`,
      exact: true,
    })
    .click();
  await expect(page.getByRole("dialog")).toContainText(
    "Browser verified synthetic replenishment",
  );
  await expect(
    page.getByRole("dialog").getByRole("heading", { name: "Request history" }),
  ).toBeVisible();
  await expect(page.getByRole("dialog").locator(".cf-timeline")).toContainText(
    "requester",
  );
  await expect(
    page.getByRole("button", { name: "Review request", exact: true }),
  ).toHaveCount(0);
});

test("clinical and inventory roles complete the browser workflow", async ({
  browser,
}) => {
  const options = {
    baseURL: process.env.CF_FRONTEND_URL || "http://127.0.0.1:5173",
  };
  const owner = await browser.newPage(options);
  await login(owner, "Requester");
  await owner.getByRole("button", { name: "New request", exact: true }).click();
  await owner
    .getByLabel("Supply resource")
    .selectOption({
      label: await owner
        .getByLabel("Supply resource")
        .locator("option")
        .filter({ hasText: "Cardiac pacing lead kit" })
        .textContent(),
    });
  await owner.getByLabel("Purpose").fill("Full browser lifecycle simulation");
  const submitted = owner.waitForResponse(
    (r) =>
      r.url().endsWith("/api/careflow/requests") &&
      r.request().method() === "POST",
  );
  await owner.getByRole("button", { name: "Submit request" }).click();
  const created = (await (await submitted).json()).data;
  await expect(owner.getByRole("status")).toContainText("submitted");
  await openRequests(owner);
  const id = `CF-${String(created.id).padStart(4, "0")}`;
  const clinical = await browser.newPage(options);
  await login(clinical, "Clinical staff");
  await openRequests(clinical);
  await clinical.getByRole("button", { name: id, exact: true }).click();
  await clinical
    .getByRole("button", { name: "Review request", exact: true })
    .click();
  await clinical
    .getByLabel("Reason")
    .fill("Synthetic need confirmed in browser");
  await clinical.getByRole("button", { name: "Record review" }).click();
  await expect(clinical.getByRole("status")).toContainText(
    "Clinical review recorded",
  );
  const manager = await browser.newPage(options);
  await login(manager, "Inventory manager");
  await openRequests(manager);
  await manager.getByRole("button", { name: id, exact: true }).click();
  await manager
    .getByRole("button", { name: "Reserve inventory", exact: true })
    .click();
  await expect(manager.getByRole("status")).toContainText(
    "reserved atomically",
  );
  await manager.getByRole("button", { name: id, exact: true }).click();
  await manager
    .getByRole("button", { name: "Confirm fulfillment", exact: true })
    .click();
  await expect(manager.getByRole("status")).toContainText(
    "Fulfillment confirmed",
  );
  await manager.getByRole("button", { name: id, exact: true }).click();
  await expect(manager.getByRole("dialog").locator(".cf-badge")).toHaveText(
    "Fulfilled",
  );
  await expect(
    manager.getByRole("dialog").locator(".cf-timeline>div"),
  ).toHaveCount(4);
  await owner.close();
  await clinical.close();
  await manager.close();
});

test("administrator can read audit records and role assignments", async ({
  page,
}) => {
  await login(page);
  await page.getByRole("button", { name: "Audit trail", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Activity ledger" }),
  ).toBeVisible();
  await expect(page.locator(".cf-timeline>div").first()).toContainText(
    "inventory",
  );
  await page
    .getByRole("button", { name: "Team & access", exact: true })
    .click();
  await expect(page.getByLabel("Role for requester")).toHaveValue("REQUESTER");
});

test("mobile workspace has no horizontal page overflow", async ({
  page,
}, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  await page.screenshot({
    path: testInfo.outputPath("mobile.png"),
    fullPage: true,
  });
});
