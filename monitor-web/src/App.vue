<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const overview = ref({ totalServers: 0, onlineServers: 0, offlineServers: 0 })
const loading = ref(false)
const backendError = ref('')
const showSubscribe = ref(false)
const email = ref('')
const subscribed = ref(false)
const subscriptionSubmitting = ref(false)
const subscriptionError = ref('')
const expandedGroups = ref([])
const reportedOverallStatus = ref(null)
const serviceGroups = ref([])
const activeIncidents = ref([])
const metricOverview = ref({ totalServers: 0, onlineServers: 0, offlineServers: 0, averageCpuUsagePercent: null })
const performance = ref({ requestCount: 0, qps: 0, serverErrorCount: 0, latency: {} })
const cacheMetrics = ref({ hits: 0, misses: 0, errors: 0, totalReads: 0, hitRate: 0 })
const serverSnapshots = ref([])
const managedServices = ref([])
const servicesLoading = ref(false)
const serviceError = ref('')
const serviceNotice = ref('')
const serviceFormVisible = ref(false)
const editingServiceId = ref(null)
const serviceSaving = ref(false)
const serviceForm = ref(createEmptyService())
let refreshTimer

const statusMeta = {
  operational: { label: '正常运行', shortLabel: '正常', icon: '✓' },
  degraded: { label: '部分服务性能下降', shortLabel: '性能下降', icon: '!' },
  major_outage: { label: '服务中断', shortLabel: '中断', icon: '×' },
  unknown: { label: '等待数据', shortLabel: '等待数据', icon: '·' },
}

function metaFor(status) {
  return statusMeta[status] || statusMeta.unknown
}

function incidentStatusLabel(status) {
  return { investigating: '调查中', identified: '已定位', monitoring: '观察中', resolved: '已恢复' }[status] || '处理中'
}

const overallStatus = computed(() => {
  if (backendError.value) return 'degraded'
  if (reportedOverallStatus.value) return reportedOverallStatus.value
  if (!overview.value.totalServers) return 'operational'
  return overview.value.onlineServers < overview.value.totalServers ? 'degraded' : 'operational'
})

const currentStatus = computed(() => metaFor(overallStatus.value))

const currentStatusCopy = computed(() => {
  if (activeIncidents.value.length > 0) return activeIncidents.value[0].message
  if (overallStatus.value === 'degraded') return '部分服务响应时间较长，正在持续观察。'
  return '所有服务运行正常。'
})

const formattedUpdatedAt = ref('刚刚')

async function fetchJson(url, options) {
  try {
    const response = await fetch(url, options)
    if (!response.ok) return null
    return await response.json()
  } catch (reason) {
    return null
  }
}

function createEmptyService() {
  return {
    serviceKey: '',
    name: '',
    groupName: 'main',
    endpointUrl: '',
    description: '',
    status: 'operational',
  }
}

function serviceStatusLabel(status) {
  return metaFor(status).label
}

async function loadManagedServices() {
  servicesLoading.value = true
  serviceError.value = ''
  const payload = await fetchJson('/api/v1/services')
  if (payload) {
    managedServices.value = payload
  } else {
    serviceError.value = '服务目录读取失败，请确认后端已启动'
  }
  servicesLoading.value = false
}

function openServiceForm(service = null) {
  editingServiceId.value = service?.id || null
  serviceForm.value = service ? { ...service } : createEmptyService()
  serviceNotice.value = ''
  serviceError.value = ''
  serviceFormVisible.value = true
}

function closeServiceForm() {
  serviceFormVisible.value = false
  editingServiceId.value = null
  serviceForm.value = createEmptyService()
}

async function saveManagedService() {
  if (serviceSaving.value) return
  serviceSaving.value = true
  serviceError.value = ''
  serviceNotice.value = ''
  const id = editingServiceId.value
  const response = await fetch(id ? `/api/v1/services/${id}` : '/api/v1/services', {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(serviceForm.value),
  }).catch(() => null)
  if (!response || !response.ok) {
    const detail = response ? await response.text().catch(() => '') : ''
    serviceError.value = detail || '保存失败，请检查后端连接和表单内容'
  } else {
    await loadManagedServices()
    serviceNotice.value = id ? '服务信息已更新' : '服务已创建'
    closeServiceForm()
  }
  serviceSaving.value = false
}

async function removeManagedService(service) {
  if (!window.confirm(`确定删除“${service.name}”吗？`)) return
  serviceError.value = ''
  const response = await fetch(`/api/v1/services/${service.id}`, { method: 'DELETE' }).catch(() => null)
  if (!response || !response.ok) {
    serviceError.value = '删除失败，请稍后重试'
    return
  }
  managedServices.value = managedServices.value.filter((item) => item.id !== service.id)
  serviceNotice.value = '服务已删除'
}

async function loadDashboard() {
  loading.value = true
  try {
    const [payload, overviewPayload, performancePayload, cachePayload, serversPayload] = await Promise.all([
      fetchJson('/api/v1/status'),
      fetchJson('/api/v1/metrics/overview'),
      fetchJson('/api/v1/metrics/summary?windowSeconds=300'),
      fetchJson('/api/v1/metrics/cache'),
      fetchJson('/api/v1/metrics/servers'),
    ])
    if (!payload) throw new Error('API 返回了错误状态')
    overview.value = payload.overview || overview.value
    reportedOverallStatus.value = payload.overallStatus || null
    serviceGroups.value = payload.groups || []
    activeIncidents.value = payload.incidents || []
    metricOverview.value = overviewPayload || metricOverview.value
    performance.value = performancePayload || performance.value
    cacheMetrics.value = cachePayload || cacheMetrics.value
    serverSnapshots.value = serversPayload || serverSnapshots.value
    backendError.value = ''
  } catch (reason) {
    backendError.value = reason.message
  } finally {
    formattedUpdatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    loading.value = false
  }
}

function displayNumber(value, digits = 1) {
  return typeof value === 'number' && Number.isFinite(value) ? value.toFixed(digits) : '—'
}

function memoryPercent(server) {
  if (!server?.memoryUsedBytes || !server?.memoryTotalBytes) return '—'
  return `${displayNumber(server.memoryUsedBytes * 100 / server.memoryTotalBytes)}%`
}

function diskPercent(server) {
  if (!server?.diskUsedBytes || !server?.diskTotalBytes) return '—'
  return `${displayNumber(server.diskUsedBytes * 100 / server.diskTotalBytes)}%`
}

function serverOnline(server) {
  if (!server?.receivedAt) return false
  return Date.now() - new Date(server.receivedAt).getTime() < 120000
}

function serverUpdatedAt(server) {
  if (!server?.receivedAt) return '暂无数据'
  return new Date(server.receivedAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function toggleGroup(groupId) {
  expandedGroups.value = expandedGroups.value.includes(groupId)
    ? expandedGroups.value.filter((id) => id !== groupId)
    : [...expandedGroups.value, groupId]
}

async function submitSubscription() {
  if (!email.value.trim() || subscriptionSubmitting.value) return
  subscriptionSubmitting.value = true
  subscriptionError.value = ''
  try {
    const response = await fetch('/api/v1/status/subscriptions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.value.trim() }),
    })
    if (!response.ok) throw new Error('订阅失败，请检查邮箱地址或稍后重试')
    subscribed.value = true
  } catch (reason) {
    subscriptionError.value = reason.message
  } finally {
    subscriptionSubmitting.value = false
  }
}

function closeSubscription() {
  showSubscribe.value = false
  subscribed.value = false
  subscriptionError.value = ''
  email.value = ''
}

onMounted(() => {
  loadDashboard()
  loadManagedServices()
  refreshTimer = setInterval(loadDashboard, 15000)
})

onBeforeUnmount(() => clearInterval(refreshTimer))
</script>

<template>
  <main class="status-page">
    <header class="status-header">
      <a class="brand" href="/" aria-label="Sentinel Monitor 首页">
        <span class="brand-mark">S</span>
        <span>Sentinel Monitor</span>
      </a>
      <button class="subscribe-button" type="button" @click="showSubscribe = true">订阅更新</button>
    </header>

    <section class="status-content">
      <div class="status-hero">
        <div class="hero-mark" :class="`hero-${overallStatus}`">{{ currentStatus.icon }}</div>
        <div>
          <p class="eyebrow">SENTINEL MONITOR STATUS</p>
          <h1>服务状态</h1>
          <p class="hero-copy">{{ currentStatusCopy }}</p>
        </div>
        <button class="refresh-link" type="button" :disabled="loading" @click="loadDashboard">
          {{ loading ? '刷新中...' : '立即刷新' }}
        </button>
      </div>

      <section class="notice-card" :class="`notice-${overallStatus}`">
        <div class="notice-title">
          <span class="notice-icon">{{ currentStatus.icon }}</span>
          <strong>{{ currentStatus.label }}</strong>
        </div>
        <p>{{ currentStatusCopy }}</p>
        <small>最后更新：{{ formattedUpdatedAt }}</small>
      </section>

      <section v-if="overallStatus === 'degraded'" class="incident-card">
        <div class="incident-heading">
          <span class="incident-dot"></span>
          <div>
            <h2>{{ activeIncidents[0]?.title || '监控 API 服务状态异常' }}</h2>
            <p>{{ activeIncidents[0]?.message || '部分服务受到影响，系统正在持续确认。' }}</p>
          </div>
          <span class="incident-label">{{ incidentStatusLabel(activeIncidents[0]?.status) }}</span>
        </div>
        <time>最近一次更新：{{ formattedUpdatedAt }}</time>
      </section>

      <section class="services-card">
        <div class="section-heading">
          <div>
            <h2>服务状态</h2>
            <p>过去 60 次检查结果</p>
          </div>
          <span class="window-label">最近 60 次检查</span>
        </div>

        <div class="service-list">
          <article v-for="group in serviceGroups" :key="group.id" class="service-group">
            <button class="service-summary" type="button" @click="toggleGroup(group.id)">
              <span class="service-state" :class="`state-${group.status}`">{{ metaFor(group.status).icon }}</span>
              <span class="service-name">{{ group.name }}</span>
              <span class="service-count">{{ group.serviceCount }} 个服务</span>
              <span class="service-uptime">{{ group.uptime }} <small>可用率</small></span>
              <span class="chevron" :class="{ expanded: expandedGroups.includes(group.id) }">⌄</span>
            </button>
            <div class="history-row" :aria-label="`${group.name} 历史状态`">
              <span
                v-for="(day, index) in group.history"
                :key="`${group.id}-${index}`"
                class="history-block"
                :class="`history-${day}`"
                :title="`${group.name} · 第 ${index + 1} 次检查 · ${metaFor(day).shortLabel}`"
              ></span>
            </div>
            <div v-if="expandedGroups.includes(group.id)" class="service-details">
              <div v-for="child in group.children" :key="child" class="detail-row">
                <span class="detail-state">✓</span>
                <span>{{ child }}</span>
                <span>{{ metaFor(group.status).shortLabel }}</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="ops-panel">
        <div class="section-heading ops-heading">
          <div>
            <h2>运维概览</h2>
            <p>最近 5 分钟 API 与服务器指标</p>
          </div>
          <span class="window-label">实时数据</span>
        </div>
        <div class="metric-cards">
          <article class="metric-card">
            <span>在线服务器</span>
            <strong>{{ metricOverview.onlineServers }}<small> / {{ metricOverview.totalServers }}</small></strong>
            <em>当前在线数量</em>
          </article>
          <article class="metric-card">
            <span>平均 CPU</span>
            <strong>{{ displayNumber(metricOverview.averageCpuUsagePercent) }}<small>%</small></strong>
            <em>所有服务器平均值</em>
          </article>
          <article class="metric-card">
            <span>QPS</span>
            <strong>{{ displayNumber(performance.qps, 3) }}</strong>
            <em>{{ performance.requestCount }} 次请求</em>
          </article>
          <article class="metric-card">
            <span>P99 延迟</span>
            <strong>{{ performance.latency?.p99Ms ?? '—' }}<small>ms</small></strong>
            <em>5xx：{{ performance.serverErrorCount }}</em>
          </article>
          <article class="metric-card">
            <span>Redis 命中率</span>
            <strong>{{ displayNumber(cacheMetrics.hitRate, 2) }}<small>%</small></strong>
            <em>{{ cacheMetrics.hits }} 命中 / {{ cacheMetrics.misses }} 未命中</em>
          </article>
        </div>
        <div class="server-table" v-if="serverSnapshots.length">
          <div class="server-table-head"><span>服务器</span><span>CPU / 内存 / 磁盘</span><span>最近上报</span></div>
          <div v-for="server in serverSnapshots" :key="server.serverId" class="server-row">
            <div class="server-identity">
              <span class="server-dot" :class="{ online: serverOnline(server) }"></span>
              <div>
                <strong>{{ server.hostname || server.serverId }}</strong>
                <small>{{ server.serverId }} · {{ server.platform || 'unknown' }}</small>
              </div>
            </div>
            <span class="server-resources">{{ displayNumber(server.cpuUsagePercent) }}% / {{ memoryPercent(server) }} / {{ diskPercent(server) }}</span>
            <span class="server-time">{{ serverUpdatedAt(server) }}</span>
          </div>
        </div>
        <div v-else class="empty-metrics">等待采集器上报服务器指标</div>
      </section>

      <section class="management-panel">
        <div class="section-heading management-heading">
          <div>
            <h2>服务目录管理</h2>
            <p>通过 Spring Boot CRUD 接口维护被监控服务</p>
          </div>
          <button class="management-add" type="button" @click="openServiceForm()">新增服务</button>
        </div>

        <p v-if="serviceError" class="management-message error">{{ serviceError }}</p>
        <p v-if="serviceNotice" class="management-message success">{{ serviceNotice }}</p>

        <form v-if="serviceFormVisible" class="service-form" @submit.prevent="saveManagedService">
          <div class="form-field">
            <label for="service-key">服务标识</label>
            <input id="service-key" v-model="serviceForm.serviceKey" required maxlength="128" placeholder="status-page" />
          </div>
          <div class="form-field">
            <label for="service-name">服务名称</label>
            <input id="service-name" v-model="serviceForm.name" required maxlength="255" placeholder="公共状态页" />
          </div>
          <div class="form-field">
            <label for="service-group">分组</label>
            <input id="service-group" v-model="serviceForm.groupName" required maxlength="128" placeholder="main" />
          </div>
          <div class="form-field form-field-wide">
            <label for="service-endpoint">接口地址</label>
            <input id="service-endpoint" v-model="serviceForm.endpointUrl" maxlength="500" placeholder="https://status.example.com/api/health" />
          </div>
          <div class="form-field form-field-wide">
            <label for="service-description">说明</label>
            <input id="service-description" v-model="serviceForm.description" maxlength="1000" placeholder="服务用途或负责人说明" />
          </div>
          <div class="form-field">
            <label for="service-status">初始状态</label>
            <select id="service-status" v-model="serviceForm.status">
              <option value="operational">正常运行</option>
              <option value="degraded">性能下降</option>
              <option value="major_outage">服务中断</option>
              <option value="unknown">等待数据</option>
            </select>
          </div>
          <div class="form-actions">
            <button class="management-add" type="submit" :disabled="serviceSaving">{{ serviceSaving ? '保存中...' : '保存' }}</button>
            <button class="management-cancel" type="button" @click="closeServiceForm">取消</button>
          </div>
        </form>

        <div v-if="servicesLoading" class="empty-metrics">服务目录加载中...</div>
        <div v-else-if="managedServices.length" class="managed-service-list">
          <div class="managed-service-head"><span>服务</span><span>分组 / 接口</span><span>状态</span><span>操作</span></div>
          <div v-for="service in managedServices" :key="service.id" class="managed-service-row">
            <div>
              <strong>{{ service.name }}</strong>
              <small>{{ service.serviceKey }}</small>
            </div>
            <div>
              <strong>{{ service.groupName }}</strong>
              <small>{{ service.endpointUrl || '未设置接口地址' }}</small>
            </div>
            <span class="managed-status" :class="`managed-status-${service.status}`">{{ serviceStatusLabel(service.status) }}</span>
            <div class="managed-actions">
              <button type="button" @click="openServiceForm(service)">编辑</button>
              <button type="button" @click="removeManagedService(service)">删除</button>
            </div>
          </div>
        </div>
        <div v-else class="empty-metrics">还没有服务，点击“新增服务”开始维护服务目录</div>
      </section>

      <footer class="status-footer">
        <span>数据每 15 秒自动刷新</span>
        <span v-if="overview.totalServers">当前监测 {{ overview.totalServers }} 台服务器</span>
        <span v-else>等待监控数据接入</span>
      </footer>
    </section>

    <div v-if="showSubscribe" class="modal-backdrop" @click.self="closeSubscription">
      <section class="subscribe-modal" role="dialog" aria-modal="true" aria-labelledby="subscribe-title">
        <button class="modal-close" type="button" aria-label="关闭" @click="closeSubscription">×</button>
        <template v-if="!subscribed">
          <p class="eyebrow">GET UPDATES</p>
          <h2 id="subscribe-title">订阅服务状态更新</h2>
          <p>当服务出现异常或恢复时，我们会向你的邮箱发送通知。</p>
          <form @submit.prevent="submitSubscription">
            <label for="email">邮箱地址</label>
            <input id="email" v-model="email" type="email" placeholder="you@example.com" required />
            <p v-if="subscriptionError" class="subscription-error">{{ subscriptionError }}</p>
            <button class="subscribe-submit" type="submit" :disabled="subscriptionSubmitting">
              {{ subscriptionSubmitting ? '保存中...' : '确认订阅' }}
            </button>
          </form>
        </template>
        <template v-else>
          <div class="success-mark">✓</div>
          <h2 id="subscribe-title">订阅已保存</h2>
          <p>后续会将服务状态通知发送到 {{ email }}。</p>
          <button class="subscribe-submit" type="button" @click="closeSubscription">完成</button>
        </template>
      </section>
    </div>
  </main>
</template>
