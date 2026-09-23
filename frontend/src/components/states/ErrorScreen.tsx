import { AppShell } from "../layout/AppShell";
import { Icon } from "../ui/Icon";

interface ErrorScreenProps {
  message: string;
  onRetry: () => void;
}

export function ErrorScreen({ message, onRetry }: ErrorScreenProps) {
  return (
    <AppShell>
      <main className="state-page error-page">
        <div className="error-code">KIX <span>×</span> ICN</div>
        <div className="error-symbol"><Icon name="cloud" /></div>
        <h1>価格データを取得できませんでした</h1>
        <p>{message}。通信状況を確認して、もう一度お試しください。</p>
        <button className="retry-button" type="button" onClick={onRetry}>
          <Icon name="refresh" />
          再読み込み
        </button>
      </main>
    </AppShell>
  );
}
