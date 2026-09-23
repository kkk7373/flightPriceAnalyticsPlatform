import { describe, expect, it } from "vitest";
import type { DailyPrice } from "../types/initResponse";
import { buildLineSegments, formatDate, formatPrice, getChartDomain } from "./dashboard";

describe("dashboard utilities", () => {
  it("formats JPY prices and preserves missing values", () => {
    expect(formatPrice(31_050, "JPY")).toBe("￥31,050");
    expect(formatPrice(null, "JPY")).toBe("—");
  });

  it("formats an API date without timezone drift", () => {
    expect(formatDate("2026-09-21")).toBe("2026年9月21日");
    expect(formatDate("2026-09-21", false)).toBe("9月21日");
  });

  it("adds a readable domain around a flat price series", () => {
    expect(getChartDomain([30_000, null, 30_000])).toEqual({ min: 27_000, max: 33_000 });
    expect(getChartDomain([null, null])).toEqual({ min: 0, max: 1 });
  });

  it("splits the price line at missing days", () => {
    const data: DailyPrice[] = [
      { date: "2026-09-18", averagePrice: 28_000, offerCount: 2 },
      { date: "2026-09-19", averagePrice: null, offerCount: 0 },
      { date: "2026-09-20", averagePrice: 31_000, offerCount: 4 },
      { date: "2026-09-21", averagePrice: 29_000, offerCount: 3 },
    ];

    expect(buildLineSegments(data, (index) => index * 10, (value) => value / 1_000)).toEqual([
      [{ x: 0, y: 28 }],
      [{ x: 20, y: 31 }, { x: 30, y: 29 }],
    ]);
  });
});
