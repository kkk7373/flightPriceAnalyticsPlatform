import type { Period, Route } from "../../types/initResponse";
import { formatDate } from "../../util/dashboard";

interface RouteOverviewProps {
  route: Route;
  period: Period;
}

export function RouteOverview({ route, period }: RouteOverviewProps) {
  return (
    <section className="route-hero" aria-labelledby="route-title">
      <div className="route-copy">
        <p className="route-caption">表示中の路線</p>
        <div className="route-line" id="route-title">
          <div><strong>{route.origin}</strong><span>関西国際空港</span></div>
          <div className="route-track" aria-hidden="true"><span className="plane">✦</span></div>
          <div><strong>{route.destination}</strong><span>仁川国際空港</span></div>
        </div>
      </div>
      <div className="period-block">
        <span>集計期間</span>
        <strong>{formatDate(period.from)} – {formatDate(period.to)}</strong>
        <small>過去30日 / {period.timezone}</small>
      </div>
    </section>
  );
}
