"use client";

import Link from "next/link";
import { MANAGERS } from "@/lib/rbac";
import { AdminPage } from "@/components/AdminPage";

export default function MarketingPage() {
  return (
    <AdminPage
      title="Marketing"
      subtitle="Demand generation for Trip On Himalaya treks"
      allowedRoles={MANAGERS}
    >
      <div className="grid gap-4 md:grid-cols-2">
        <Link
          href="/ad-channels"
          className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300"
        >
          <h2 className="text-sm font-semibold text-slate-900">Ad Channels</h2>
          <p className="mt-1 text-sm text-slate-600">
            Review authorized Meta ad accounts connected through the Marketing API.
          </p>
        </Link>
        <Link
          href="/campaigns"
          className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300"
        >
          <h2 className="text-sm font-semibold text-slate-900">Campaigns</h2>
          <p className="mt-1 text-sm text-slate-600">
            List campaign status/objective and pull spend, clicks, CPC, CPM and CTR.
          </p>
        </Link>
        <Link
          href="/leads"
          className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300 md:col-span-2"
        >
          <h2 className="text-sm font-semibold text-slate-900">Leads by source</h2>
          <p className="mt-1 text-sm text-slate-600">
            Instagram, Meta Ads and website volume live in Leads Management until Lead Ads
            webhook ingestion ships.
          </p>
        </Link>
      </div>
    </AdminPage>
  );
}
