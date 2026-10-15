"use client";

import { MANAGERS } from "@/lib/rbac";
import { AdminPage } from "@/components/AdminPage";
import { MetaAdChannelsPanel } from "@/components/marketing/MetaAdChannelsPanel";

export default function AdChannelsPage() {
  return (
    <AdminPage
      title="Ad Channels"
      subtitle="Connected Meta ad accounts and channel intake notes"
      allowedRoles={MANAGERS}
    >
      <MetaAdChannelsPanel />
    </AdminPage>
  );
}
