import { chromium } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const directory = new URL("../../docs/screenshots/", import.meta.url);
await mkdir(directory, { recursive: true });
const browser = await chromium.launch({
  ...(process.env.CF_CHROME_PATH ? { executablePath: process.env.CF_CHROME_PATH } : {}),
});
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await page.goto(process.env.CF_FRONTEND_URL || "http://127.0.0.1:5173");
  await page.screenshot({ path: fileURLToPath(new URL("login.png", directory)), fullPage: true });
  await page.getByRole("button", { name: "Administrator", exact: true }).click();
  await page.getByRole("button", { name: "Sign in to workspace" }).click();
  await page.locator(".cf-metric>strong").first().waitFor();
  await page.screenshot({ path: fileURLToPath(new URL("overview.png", directory)), fullPage: true });
  await page.getByRole("navigation").getByRole("button", { name: "Inventory", exact: true }).click();
  await page.getByText("Cardiac pacing lead kit", { exact: true }).waitFor();
  await page.screenshot({ path: fileURLToPath(new URL("inventory.png", directory)), fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("navigation").getByRole("button", { name: "Overview", exact: true }).click();
  await page.screenshot({ path: fileURLToPath(new URL("mobile.png", directory)), fullPage: true });
  console.log("Captured the running synthetic demo into docs/screenshots.");
} finally {
  await browser.close();
}
