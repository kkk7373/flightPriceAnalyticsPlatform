import { AppShell } from "../layout/AppShell";

export function LoadingScreen() {
  return (
    <AppShell>
      <main className="state-page loading-page" aria-busy="true" aria-live="polite">
        <div className="loading-route" aria-hidden="true"><span>KIX</span><div><i /></div><span>ICN</span></div>
        <div className="loader-plane" aria-hidden="true">✦</div>
        <h1>運賃データを集計しています</h1>
        <p>Athenaから過去30日分の価格を取得中です。少し時間がかかることがあります。</p>
        <div className="loading-progress" aria-hidden="true"><span /></div>
        <span className="sr-only">読み込み中</span>
      </main>
    </AppShell>
  );
}
