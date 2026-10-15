"use client";

import { MANAGERS } from "@/lib/rbac";
import { AdminPage } from "@/components/AdminPage";
import { MetaCampaignsPanel } from "@/components/marketing/MetaCampaignsPanel";

export default function CampaignsPage() {
  return (
    <AdminPage
      title="Campaigns"
      subtitle="Meta Ads campaigns and performance for authorized accounts"
      allowedRoles={MANAGERS}
    >
      <MetaCampaignsPanel />
    </AdminPage>
  );
}
