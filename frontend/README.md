# Flight Price Analytics Frontend

KIX → ICN の航空券価格を可視化する React / TypeScript ダッシュボードです。初期表示で `GET /api/init` を呼び出し、次の情報を表示します。

- 過去30日の日別平均価格（欠損日は線を分割）
- 本日出発分の価格レンジと取得オファー数
- 航空会社別の平均価格ランキング
- 出発時間帯別の平均価格
- ローディング、通信エラー、空データの各状態

## ローカル起動

Backend を `http://localhost:8080` で起動してから実行します。別URLを使う場合は `BACKEND_SERVER` を指定してください。

```bash
npm install
npm run dev
```

Vite は `/api` を Backend へプロキシします。

## 検証

```bash
npm run test
npm run lint
npm run build
```
