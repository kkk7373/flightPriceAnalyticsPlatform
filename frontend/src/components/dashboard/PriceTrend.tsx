import { useId } from "react";
import type { DailyPrice } from "../../types/initResponse";
import { buildLineSegments, formatDate, formatPrice, getChartDomain } from "../../util/dashboard";
import { PanelHeader } from "../ui/PanelHeader";

const chartWidth = 760;
const chartHeight = 250;
const padding = { top: 24, right: 14, bottom: 36, left: 58 };

export function PriceTrend({ data, currency }: { data: DailyPrice[]; currency: string }) {
  const gradientId = useId().replace(/:/g, "");
  const domain = getChartDomain(data.map((item) => item.averagePrice));
  const x = (index: number) => padding.left + (index / Math.max(data.length - 1, 1)) * (chartWidth - padding.left - padding.right);
  const y = (value: number) => padding.top + ((domain.max - value) / (domain.max - domain.min)) * (chartHeight - padding.top - padding.bottom);
  const segments = buildLineSegments(data, x, y);
  const ticks = [domain.max, (domain.max + domain.min) / 2, domain.min];
  const validData = data.filter((item) => item.averagePrice !== null);
  const last = validData.at(-1);
  const dateLabelIndexes = [0, Math.floor((data.length - 1) / 2), data.length - 1]
    .filter((value, index, values) => value >= 0 && values.indexOf(value) === index);

  return (
    <article className="panel trend-panel">
      <PanelHeader
        title="価格の推移"
        description="出発日別の平均価格"
        meta={last ? `直近 ${formatPrice(last.averagePrice, currency)}` : "データなし"}
        inverted
      />
      <div className="chart-wrap">
        {validData.length === 0 ? (
          <ChartEmpty />
        ) : (
          <svg className="line-chart" viewBox={`0 0 ${chartWidth} ${chartHeight}`} role="img" aria-label="過去30日の日別平均価格推移">
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stopColor="#b8f34a" stopOpacity="0.28" />
                <stop offset="1" stopColor="#b8f34a" stopOpacity="0" />
              </linearGradient>
            </defs>
            {ticks.map((tick) => (
              <g key={tick}>
                <line x1={padding.left} y1={y(tick)} x2={chartWidth - padding.right} y2={y(tick)} className="grid-line" />
                <text x={padding.left - 10} y={y(tick) + 4} textAnchor="end" className="axis-label">¥{Math.round(tick / 1000)}k</text>
              </g>
            ))}
            {segments.map((segment, index) => {
              const line = segment.map((point) => `${point.x},${point.y}`).join(" ");
              const area = `${segment[0].x},${chartHeight - padding.bottom} ${line} ${segment.at(-1)!.x},${chartHeight - padding.bottom}`;
              return <g key={index}><polygon points={area} fill={`url(#${gradientId})`} /><polyline points={line} className="price-line" /></g>;
            })}
            {validData.map((item) => {
              const originalIndex = data.indexOf(item);
              return (
                <circle key={item.date} cx={x(originalIndex)} cy={y(item.averagePrice!)} r="3" className="price-point">
                  <title>{formatDate(item.date)}: {formatPrice(item.averagePrice, currency)}</title>
                </circle>
              );
            })}
            {dateLabelIndexes.map((index) => (
              <text
                key={index}
                x={x(index)}
                y={chartHeight - 8}
                textAnchor={index === 0 ? "start" : index === data.length - 1 ? "end" : "middle"}
                className="axis-label date-label"
              >
                {formatDate(data[index].date, false)}
              </text>
            ))}
          </svg>
        )}
      </div>
      <p className="chart-footnote">欠損日は線をつながず、価格0円として扱いません。</p>
    </article>
  );
}

function ChartEmpty() {
  return <div className="chart-empty"><span className="empty-wave" aria-hidden="true" /><p>価格データを待っています</p></div>;
}
