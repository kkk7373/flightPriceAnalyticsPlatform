import type { ReactNode } from "react";

export type IconName =
  | "dashboard"
  | "route"
  | "bell"
  | "spark"
  | "info"
  | "plane"
  | "cloud"
  | "refresh";

const paths: Record<IconName, ReactNode> = {
  dashboard: <><rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/></>,
  route: <><circle cx="5" cy="18" r="2"/><circle cx="19" cy="6" r="2"/><path d="M7 18c7 0 3-12 10-12"/></>,
  bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></>,
  spark: <path d="m12 2 1.8 6.2L20 10l-6.2 1.8L12 18l-1.8-6.2L4 10l6.2-1.8L12 2Z"/>,
  info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7h.01"/></>,
  plane: <><path d="m3 13 18-8-8 18-2-8-8-2Z"/><path d="m11 15 5-5"/></>,
  cloud: <path d="M7 18h10a4 4 0 0 0 .4-8A6 6 0 0 0 6 8a5 5 0 0 0 1 10Z"/>,
  refresh: <><path d="M20 6v5h-5"/><path d="M18.5 15a7 7 0 1 1-.7-7.8L20 11"/></>,
};

export function Icon({ name }: { name: IconName }) {
  return <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">{paths[name]}</svg>;
}
