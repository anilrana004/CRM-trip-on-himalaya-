"use client";

import { useCallback, useEffect, useState } from "react";
import {
  api,
  formatApiError,
  type MetaAdAccount,
  type MetaCampaign,
  type MetaInsightRow,
} from "@/lib/api";

const ACCOUNT_KEY = "toh_meta_ad_account";

const PRESETS = [
  { value: "today", label: "Today" },
  { value: "yesterday", label: "Yesterday" },
  { value: "last_7d", label: "Last 7 days" },
  { value: "last_14d", label: "Last 14 days" },
  { value: "last_30d", label: "Last 30 days" },
  { value: "this_month", label: "This month" },
  { value: "last_month", label: "Last month" },
];

function statusTone(status?: string | null) {
  const s = (status || "").toUpperCase();
  if (s === "ACTIVE") return "bg-emerald-50 text-emerald-800";
  if (s === "PAUSED") return "bg-amber-50 text-amber-800";
  if (s.includes("DELETE") || s === "ARCHIVED") return "bg-slate-100 text-slate-600";
  return "bg-slate-50 text-slate-700";
}

function fmtNum(v?: string | null) {
  if (v == null || v === "") return "—";
  const n = Number(v);
  if (Number.isNaN(n)) return v;
  return n.toLocaleString(undefined, { maximumFractionDigits: 2 });
}

export function MetaCampaignsPanel() {
  const [accounts, setAccounts] = useState<MetaAdAccount[]>([]);
  const [accountId, setAccountId] = useState("");
  const [campaigns, setCampaigns] = useState<MetaCampaign[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [insights, setInsights] = useState<MetaInsightRow[]>([]);
  const [preset, setPreset] = useState("last_7d");
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [loadingCampaigns, setLoadingCampaigns] = useState(false);
  const [loadingInsights, setLoadingInsights] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [syncedAt, setSyncedAt] = useState<string | null>(null);

  const loadAccounts = useCallback(async () => {
    setLoadingAccounts(true);
    setError(null);
    try {
      const list = await api.listMetaAdAccounts();
      setAccounts(list);
      const stored = typeof window !== "undefined" ? localStorage.getItem(ACCOUNT_KEY) : null;
      const initial =
        (stored && list.some((a) => a.id === stored) && stored) || list[0]?.id || "";
      setAccountId(initial);
      if (initial && typeof window !== "undefined") {
        localStorage.setItem(ACCOUNT_KEY, initial);
      }
    } catch (err) {
      setError(formatApiError(err, "Could not load Meta ad accounts"));
      setAccounts([]);
    } finally {
      setLoadingAccounts(false);
    }
  }, []);

  const loadCampaigns = useCallback(
    async (after?: string | null, append = false) => {
      if (!accountId) return;
      setLoadingCampaigns(true);
      setError(null);
      try {
        const page = await api.listMetaCampaigns(accountId, {
          after: after || undefined,
          limit: 25,
        });
        setCampaigns((prev) => (append ? [...prev, ...page.data] : page.data));
        setCursor(page.nextCursor ?? null);
        setHasMore(!!page.hasMore);
        setSyncedAt(new Date().toISOString());
      } catch (err) {
        setError(formatApiError(err, "Could not load campaigns"));
        if (!append) setCampaigns([]);
      } finally {
        setLoadingCampaigns(false);
      }
    },
    [accountId],
  );

  const loadInsights = useCallback(async () => {
    if (!accountId) return;
    setLoadingInsights(true);
    setError(null);
    try {
      const res = await api.getMetaInsights(accountId, { datePreset: preset });
      setInsights(res.rows);
      setSyncedAt(new Date().toISOString());
    } catch (err) {
      setError(formatApiError(err, "Could not load insights"));
      setInsights([]);
    } finally {
      setLoadingInsights(false);
    }
  }, [accountId, preset]);

  useEffect(() => {
    void loadAccounts();
  }, [loadAccounts]);

  useEffect(() => {
    if (!accountId) return;
    void loadCampaigns(null, false);
    void loadInsights();
  }, [accountId, loadCampaigns, loadInsights]);

  function onAccountChange(id: string) {
    setAccountId(id);
    if (typeof window !== "undefined") localStorage.setItem(ACCOUNT_KEY, id);
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3">
        <label className="min-w-[220px] flex-1 text-sm">
          <span className="mb-1 block font-medium text-slate-700">Ad account</span>
          <select
            value={accountId}
            disabled={loadingAccounts || accounts.length === 0}
            onChange={(e) => onAccountChange(e.target.value)}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-slate-900"
          >
            {accounts.length === 0 && <option value="">No accounts</option>}
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name} ({a.id}){a.currency ? ` · ${a.currency}` : ""}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm">
          <span className="mb-1 block font-medium text-slate-700">Insights range</span>
          <select
            value={preset}
            onChange={(e) => setPreset(e.target.value)}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-slate-900"
          >
            {PRESETS.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label}
              </option>
            ))}
          </select>
        </label>

        <button
          type="button"
          onClick={() => {
            void loadCampaigns(null, false);
            void loadInsights();
          }}
          disabled={!accountId || loadingCampaigns || loadingInsights}
          className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800 disabled:opacity-50"
        >
          Refresh
        </button>

        {syncedAt && (
          <p className="ml-auto text-xs text-slate-400">
            Last sync {new Date(syncedAt).toLocaleString()}
          </p>
        )}
      </div>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {loadingAccounts && (
        <p className="text-sm text-slate-500">Loading authorized Meta ad accounts…</p>
      )}

      {!loadingAccounts && accounts.length === 0 && !error && (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white px-4 py-8 text-center text-sm text-slate-600">
          No Meta ad accounts available. Set <code className="text-xs">META_ACCESS_TOKEN</code> on
          the backend and ensure the system user can access your Business ad accounts.
        </div>
      )}

      {accountId && (
        <>
          <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
            <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
              <h2 className="text-sm font-semibold text-slate-900">Campaigns</h2>
              {loadingCampaigns && <span className="text-xs text-slate-400">Loading…</span>}
            </div>
            {campaigns.length === 0 && !loadingCampaigns ? (
              <p className="px-4 py-8 text-center text-sm text-slate-500">No campaigns in this account.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-4 py-2 font-medium">Name</th>
                      <th className="px-4 py-2 font-medium">Status</th>
                      <th className="px-4 py-2 font-medium">Objective</th>
                      <th className="px-4 py-2 font-medium">Updated</th>
                    </tr>
                  </thead>
                  <tbody>
                    {campaigns.map((c) => (
                      <tr key={c.id} className="border-t border-slate-100">
                        <td className="px-4 py-2.5">
                          <div className="font-medium text-slate-900">{c.name || "—"}</div>
                          <div className="text-xs text-slate-400">{c.id}</div>
                        </td>
                        <td className="px-4 py-2.5">
                          <span
                            className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${statusTone(
                              c.effectiveStatus || c.status,
                            )}`}
                          >
                            {c.effectiveStatus || c.status || "—"}
                          </span>
                        </td>
                        <td className="px-4 py-2.5 text-slate-700">{c.objective || "—"}</td>
                        <td className="px-4 py-2.5 text-slate-600">
                          {c.updatedTime ? new Date(c.updatedTime).toLocaleString() : "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {hasMore && (
              <div className="border-t border-slate-100 px-4 py-3">
                <button
                  type="button"
                  disabled={loadingCampaigns}
                  onClick={() => void loadCampaigns(cursor, true)}
                  className="text-sm font-medium text-slate-900 underline-offset-2 hover:underline disabled:opacity-50"
                >
                  Load more
                </button>
              </div>
            )}
          </section>

          <section className="overflow-hidden rounded-xl border border-slate-200 bg-white">
            <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
              <h2 className="text-sm font-semibold text-slate-900">Performance</h2>
              {loadingInsights && <span className="text-xs text-slate-400">Loading…</span>}
            </div>
            {insights.length === 0 && !loadingInsights ? (
              <p className="px-4 py-8 text-center text-sm text-slate-500">
                No insight rows for this range (empty Meta result, not a silent failure).
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-4 py-2 font-medium">Campaign</th>
                      <th className="px-4 py-2 font-medium">Impr.</th>
                      <th className="px-4 py-2 font-medium">Clicks</th>
                      <th className="px-4 py-2 font-medium">Spend</th>
                      <th className="px-4 py-2 font-medium">CPC</th>
                      <th className="px-4 py-2 font-medium">CPM</th>
                      <th className="px-4 py-2 font-medium">CTR</th>
                      <th className="px-4 py-2 font-medium">Dates</th>
                    </tr>
                  </thead>
                  <tbody>
                    {insights.map((row, i) => (
                      <tr key={`${row.campaignId ?? "row"}-${i}`} className="border-t border-slate-100">
                        <td className="px-4 py-2.5 font-medium text-slate-900">
                          {row.campaignName || row.campaignId || "—"}
                        </td>
                        <td className="px-4 py-2.5">{fmtNum(row.impressions)}</td>
                        <td className="px-4 py-2.5">{fmtNum(row.clicks)}</td>
                        <td className="px-4 py-2.5">{fmtNum(row.spend)}</td>
                        <td className="px-4 py-2.5">{fmtNum(row.cpc)}</td>
                        <td className="px-4 py-2.5">{fmtNum(row.cpm)}</td>
                        <td className="px-4 py-2.5">{fmtNum(row.ctr)}</td>
                        <td className="px-4 py-2.5 text-xs text-slate-500">
                          {row.dateStart || "—"} → {row.dateStop || "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
