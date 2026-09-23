import type { DepartureTimeBand } from "../../types/initResponse";
import { formatPrice } from "../../util/dashboard";
import { PanelHeader } from "../ui/PanelHeader";

const timeBandLabels = {
  earlyMorning: { label: "早朝", hours: "00–06" },
  morning: { label: "午前", hours: "06–12" },
  afternoon: { label: "午後", hours: "12–18" },
  evening: { label: "夜", hours: "18–24" },
} as const;

interface TimeBandChartProps {
  data: DepartureTimeBand[];
  currency: string;
}

export function TimeBandChart({ data, currency }: TimeBandChartProps) {
  const validPrices = data.flatMap((item) => item.averagePrice === null ? [] : [item.averagePrice]);
  const max = Math.max(...validPrices, 1);
  const cheapest = data
    .filter((item) => item.averagePrice !== null)
    .sort((a, b) => a.averagePrice! - b.averagePrice!)[0]?.timeBand;

  return (
    <article className="panel time-panel">
      <PanelHeader title="安い出発時間帯" description="過去30日の平均価格" />
      <div className="time-bars">
        {data.map((item) => {
          const label = timeBandLabels[item.timeBand];
          const width = item.averagePrice === null ? "0%" : `${Math.max(8, (item.averagePrice / max) * 100)}%`;

          return (
            <div className={`time-row${item.timeBand === cheapest ? " is-cheapest" : ""}`} key={item.timeBand}>
              <div className="time-label"><strong>{label.label}</strong><span>{label.hours}</span></div>
              <div className="bar-track"><span style={{ width }} /></div>
              <div className="time-value"><strong>{formatPrice(item.averagePrice, currency)}</strong><span>{item.offerCount}件</span></div>
            </div>
          );
        })}
      </div>
      <p className="panel-note">観測された平均価格の比較であり、将来価格の予測ではありません。</p>
    </article>
  );
}
