import type { DailyPrice } from "../types/initResponse";

export function formatPrice(value: number | null, currency: string): string {
  if (value === null) return "—";
  return new Intl.NumberFormat("ja-JP", { style: "currency", currency, maximumFractionDigits: 0 }).format(value);
}

export function formatDate(value: string, includeYear = true): string {
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) return value;
  return new Intl.DateTimeFormat("ja-JP", { year: includeYear ? "numeric" : undefined, month: "short", day: "numeric" }).format(new Date(Date.UTC(year, month - 1, day)));
}

export function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("ja-JP", { timeZone: "Asia/Tokyo", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(date);
}

export function getChartDomain(values: Array<number | null>): { min: number; max: number } {
  const valid = values.filter((value): value is number => value !== null && Number.isFinite(value));
  if (valid.length === 0) return { min: 0, max: 1 };
  const min = Math.min(...valid);
  const max = Math.max(...valid);
  if (min === max) {
    const padding = Math.max(min * 0.1, 1);
    return { min: Math.max(0, min - padding), max: max + padding };
  }
  const padding = (max - min) * 0.18;
  return { min: Math.max(0, min - padding), max: max + padding };
}

export function buildLineSegments(data: DailyPrice[], x: (index: number) => number, y: (value: number) => number): Array<Array<{ x: number; y: number }>> {
  const segments: Array<Array<{ x: number; y: number }>> = [];
  data.forEach((item, index) => {
    if (item.averagePrice === null) return;
    if (index === 0 || data[index - 1].averagePrice === null) segments.push([]);
    segments.at(-1)!.push({ x: x(index), y: y(item.averagePrice) });
  });
  return segments;
}
