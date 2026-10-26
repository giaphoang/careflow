import React from "react";

const paths = {
  grid: "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z",
  box: "m12 3 9 5v8l-9 5-9-5V8l9-5z M3 8l9 5 9-5 M12 13v8 M7.5 5.5l9 5",
  requests: "M8 4H5v17h14V4h-3 M8 2h8v5H8z M8 12h8 M8 16h5",
  shield: "m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3z m-4 9 3 3 5-6",
  chart: "M4 3v18h17 M8 16v-4 M13 16V7 M18 16v-7",
  arrow: "M4 12h16 m-6-6 6 6-6 6",
  plus: "M12 5v14 M5 12h14",
  search: "M21 21l-5-5 M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0",
  bell: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9 M10 21h4",
  check: "m5 12 4 4L19 6",
  clock: "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0 M12 7v5l3 2",
  logout: "M9 4H4v16h5 M10 12h11 m-4-4 4 4-4 4",
  close: "m6 6 12 12 M6 18 18 6",
  users:
    "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M20 8v6 M17 11h6",
  download: "M12 3v12 m-5-5 5 5 5-5 M4 16v5h16v-5",
  pulse: "M2 12h5l3-8 4 16 3-8h5",
};
export default function Icon({ name, size = 20, ...props }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      <path d={paths[name] || paths.box} />
    </svg>
  );
}
