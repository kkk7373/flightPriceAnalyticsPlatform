import type { AirlineRanking as AirlineRankingData } from "../../types/initResponse";
import { formatPrice } from "../../util/dashboard";
import { Icon } from "../ui/Icon";
import { PanelHeader } from "../ui/PanelHeader";

interface AirlineRankingProps {
  data: AirlineRankingData[];
  currency: string;
}

export function AirlineRanking({ data, currency }: AirlineRankingProps) {
  return (
    <article className="panel airline-panel">
      <PanelHeader title="航空会社ランキング" description="平均価格が安い順" meta={`${data.length}社`} />
      {data.length === 0 ? (
        <TableEmpty />
      ) : (
        <div className="ranking-table" role="table" aria-label="航空会社別平均価格ランキング">
          <div className="ranking-head" role="row"><span>順位 / 航空会社</span><span>オファー</span><span>平均価格</span></div>
          {data.slice(0, 6).map((airline, index) => (
            <div className="ranking-row" role="row" key={airline.carrierCode}>
              <div className="airline-name">
                <span className="rank">{String(index + 1).padStart(2, "0")}</span>
                <span className="carrier-badge">{airline.carrierCode}</span>
                <strong>{airline.carrierName}</strong>
              </div>
              <span>{airline.offerCount.toLocaleString("ja-JP")}件</span>
              <strong>{formatPrice(airline.averagePrice, currency)}</strong>
            </div>
          ))}
        </div>
      )}
    </article>
  );
}

function TableEmpty() {
  return <div className="table-empty"><Icon name="plane" /><p>ランキングを表示できる航空会社データがありません。</p></div>;
}
