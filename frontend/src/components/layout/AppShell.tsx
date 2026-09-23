import type { ReactNode } from "react";
import logo from "../../assets/flight-price-logo.svg";
import { Icon } from "../ui/Icon";

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#main" aria-label="Flight scope ホーム">
          <span className="brand-mark"><img src={logo} alt="" /></span>
          <span>Flight scope</span>
        </a>
        <nav className="primary-nav" aria-label="メインナビゲーション">
          <a className="nav-item is-active" href="#main" aria-current="page">
            <Icon name="dashboard" />
            <span>ダッシュボード</span>
          </a>
          <span className="nav-item is-disabled" aria-disabled="true">
            <Icon name="route" />
            <span>路線を比較</span>
            <small>準備中</small>
          </span>
          <span className="nav-item is-disabled" aria-disabled="true">
            <Icon name="bell" />
            <span>価格アラート</span>
            <small>準備中</small>
          </span>
        </nav>
        <div className="sidebar-note">
          <Icon name="spark" />
          <p>価格は検証済みオファーを集計しています。</p>
        </div>
      </aside>
      <div className="content-frame">{children}</div>
    </div>
  );
}
