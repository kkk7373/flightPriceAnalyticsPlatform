import type { Today } from "../../types/initResponse";
import { formatPrice } from "../../util/dashboard";

interface MetricsStripProps {
  today: Today;
  currency: string;
}

export function MetricsStrip({ today, currency }: MetricsStripProps) {
  return (
    <section className="metrics-strip" aria-label="本日の価格サマリー">
      <Metric label="本日の平均価格" value={formatPrice(today.averagePrice, currency)} emphasis />
      <Metric label="最安価格" value={formatPrice(today.lowestPrice, currency)} />
      <Metric label="最高価格" value={formatPrice(today.highestPrice, currency)} />
      <Metric label="取得オファー数" value={`${today.offerCount.toLocaleString("ja-JP")}件`} />
    </section>
  );
}

function Metric({ label, value, emphasis = false }: { label: string; value: string; emphasis?: boolean }) {
  return (
    <div className={`metric${emphasis ? " metric-emphasis" : ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
