import type { InitResponse } from "../../types/initResponse";
import { formatDateTime } from "../../util/dashboard";
import { AppShell } from "../layout/AppShell";
import { Icon } from "../ui/Icon";
import { AirlineRanking } from "./AirlineRanking";
import { MetricsStrip } from "./MetricsStrip";
import { PriceRange } from "./PriceRange";
import { PriceTrend } from "./PriceTrend";
import { RouteOverview } from "./RouteOverview";
import { TimeBandChart } from "./TimeBandChart";

export function Dashboard({ data }: { data: InitResponse }) {
  const hasAnyPrice = data.dailyPrices.some((item) => item.averagePrice !== null);

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <p className="breadcrumb">フライト価格分析</p>
          <h1>関西からソウルへ</h1>
        </div>
        <div className="updated-at">
          <span className="live-dot" aria-hidden="true" />
          <span>更新 {formatDateTime(data.lastUpdatedAt)}</span>
        </div>
      </header>

      <main id="main" className="dashboard">
        <RouteOverview route={data.route} period={data.period} />
        <MetricsStrip today={data.today} currency={data.currency} />

        {!hasAnyPrice && (
          <div className="data-notice" role="status">
            <Icon name="info" />
            <span>この期間の価格データはまだありません。収集後にチャートへ反映されます。</span>
          </div>
        )}

        <div className="dashboard-grid">
          <PriceTrend data={data.dailyPrices} currency={data.currency} />
          <PriceRange today={data.today} currency={data.currency} />
          <AirlineRanking data={data.airlineRanking} currency={data.currency} />
          <TimeBandChart data={data.departureTimeBands} currency={data.currency} />
        </div>
      </main>
    </AppShell>
  );
}
