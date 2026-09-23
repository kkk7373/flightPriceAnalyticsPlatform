import type { Today } from "../../types/initResponse";
import { formatDate, formatPrice } from "../../util/dashboard";
import { PanelHeader } from "../ui/PanelHeader";

interface PriceRangeProps {
  today: Today;
  currency: string;
}

export function PriceRange({ today, currency }: PriceRangeProps) {
  const { lowestPrice: low, averagePrice: average, highestPrice: high } = today;
  const hasRange = low !== null && high !== null;
  const averagePosition = hasRange && average !== null && high !== low
    ? ((average - low) / (high - low)) * 100
    : 50;

  return (
    <article className="panel range-panel">
      <PanelHeader title="本日の価格レンジ" description={`${formatDate(today.departureDate)} 出発`} />
      <div className="range-visual">
        <div className="range-orbit" aria-hidden="true">
          <span className="range-average">{formatPrice(average, currency)}</span>
        </div>
        {hasRange ? (
          <div className="range-scale">
            <span className="average-pin" style={{ left: `${Math.min(96, Math.max(4, averagePosition))}%` }} />
            <div><span>最安</span><strong>{formatPrice(low, currency)}</strong></div>
            <div className="range-rule" />
            <div><span>最高</span><strong>{formatPrice(high, currency)}</strong></div>
          </div>
        ) : (
          <p className="empty-copy">本日出発分の価格はまだありません。</p>
        )}
      </div>
      <p className="panel-note">取得した検証済みオファー {today.offerCount.toLocaleString("ja-JP")}件</p>
    </article>
  );
}
