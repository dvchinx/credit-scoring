import type { ReactNode } from "react";
import { Link } from "react-router";

export function PageHeader({
  title,
  subtitle,
  badge,
  actions,
  back,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  badge?: ReactNode;
  actions?: ReactNode;
  back?: { to: string; label: string };
}) {
  return (
    <header className="page-header">
      <div>
        {back && (
          <Link className="back" to={back.to}>
            ← {back.label}
          </Link>
        )}
        <div className="title-row">
          <h1>{title}</h1>
          {badge}
        </div>
        {subtitle && <p className="subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="row">{actions}</div>}
    </header>
  );
}
