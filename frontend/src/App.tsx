import { Dashboard } from "./components/dashboard/Dashboard";
import { ErrorScreen } from "./components/states/ErrorScreen";
import { LoadingScreen } from "./components/states/LoadingScreen";
import { useDashboardInit } from "./hooks/useDashboardInit";

function App() {
  const dashboard = useDashboardInit();

  if (dashboard.status === "loading") {
    return <LoadingScreen />;
  }

  if (dashboard.status === "error") {
    return <ErrorScreen message={dashboard.error.message} onRetry={dashboard.retry} />;
  }

  return <Dashboard data={dashboard.data} />;
}

export default App;
