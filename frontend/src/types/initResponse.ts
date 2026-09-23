/** YYYY-MM-DD形式の日付文字列 */
export type IsoDate = string;

/** ISO 8601形式の日時文字列 */
export type IsoDateTime = string;

export type DepartureTimeBandName =
  | "earlyMorning"
  | "morning"
  | "afternoon"
  | "evening";

export interface Route {
  origin: string;
  destination: string;
}

export interface Period {
  from: IsoDate;
  to: IsoDate;
  timezone: string;
}

export interface Today {
  departureDate: IsoDate;
  offerCount: number;
  averagePrice: number | null;
  lowestPrice: number | null;
  highestPrice: number | null;
}

export interface DailyPrice {
  date: IsoDate;
  averagePrice: number | null;
  offerCount: number;
}

export interface AirlineRanking {
  carrierCode: string;
  carrierName: string;
  averagePrice: number;
  offerCount: number;
}

export interface DepartureTimeBand {
  timeBand: DepartureTimeBandName;
  averagePrice: number | null;
  offerCount: number;
}

export interface InitResponse {
  route: Route;
  period: Period;
  currency: string;
  lastUpdatedAt: IsoDateTime;
  today: Today;
  dailyPrices: DailyPrice[];
  airlineRanking: AirlineRanking[];
  departureTimeBands: DepartureTimeBand[];
}
