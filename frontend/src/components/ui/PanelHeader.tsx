interface PanelHeaderProps {
  title: string;
  description: string;
  meta?: string;
  inverted?: boolean;
}

export function PanelHeader({ title, description, meta, inverted = false }: PanelHeaderProps) {
  return (
    <header className={`panel-header${inverted ? " is-inverted" : ""}`}>
      <div>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
      {meta && <span>{meta}</span>}
    </header>
  );
}
