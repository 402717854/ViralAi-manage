<template>
  <div class="app-container home" v-loading="loading">
    <!-- 顶栏：账期是整套对账体系的时间锚点，常驻可见 -->
    <div class="home-head">
      <div class="head-left">
        <div class="greet">{{ greeting }}，{{ nickName }}</div>
        <div class="head-sub">
          <span>账期 {{ billCycle }}</span>
          <span v-if="loadedAt"> · 数据更新 {{ loadedAt }}</span>
          <span v-if="canStat"> · 金额单位：元</span>
        </div>
      </div>
      <div class="head-actions">
        <el-date-picker
          v-model="billCycle"
          type="month"
          value-format="YYYY-MM"
          :clearable="false"
          style="width: 140px"
          @change="loadAll"
        />
        <el-button icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <!-- 指标卡：金额保留 2 位小数，8 位全精度放 title，避免 4 列网格折行 -->
    <el-row :gutter="12">
      <el-col :xs="12" :sm="12" :md="6" v-for="card in cards" :key="card.label">
        <div class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :class="card.cls" :title="card.full || ''">{{ card.value }}</div>
          <div class="stat-sub">{{ card.sub }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 三分区布局：左列榜单（成本 / 余额），右列动作（快捷入口 / 待办），趋势独占底部整行 -->
    <el-row :gutter="12" class="content-row">
      <el-col :xs="24" :lg="15">
        <div class="col-stack">
          <el-card shadow="never" class="panel">
          <template #header>
            <div class="card-head">
              <span>客户 × 渠道 成本 TOP5</span>
              <el-link
                v-if="canStat"
                type="primary"
                :underline="false"
                @click="go('reconcile')"
              >对账看板 ›</el-link>
            </div>
          </template>
          <div v-if="topCustomers.length" class="rank-list">
            <div
              class="rank-row"
              v-for="(item, idx) in topCustomers"
              :key="(item.customerId || 0) + '_' + (item.supplierId || 0)"
            >
              <span class="rank-idx">{{ idx + 1 }}</span>
              <span class="rank-name" :title="item.customerName || '未归属客户'">
                {{ item.customerName || "未归属客户" }}
              </span>
              <span class="rank-channel" :title="item.supplierName || ''">{{ item.supplierName || "-" }}</span>
              <span class="rank-bar"><span class="rank-bar-inner" :style="{ width: barWidth(item) }"></span></span>
              <span class="rank-amount" :title="fullAmount(item.totalCostAmount)">
                {{ shortAmount(item.totalCostAmount) }}
              </span>
              <span class="rank-rate">{{ shareRate(item) }}</span>
            </div>
          </div>
          <el-empty v-else :image-size="56" description="本账期暂无成本数据" />
          </el-card>

          <!-- 客户充值余额 TOP10：余额为 0 的客户不参与排名，避免榜单被长尾零值淹没 -->
          <el-card v-if="canFund" shadow="never" class="panel">
          <template #header>
            <div class="card-head">
              <span>客户充值余额 TOP10</span>
              <div class="head-right">
                <span class="head-hint" v-if="balanceTopTotal">共 {{ balanceTopTotal }} 个客户有余额</span>
                <el-link type="primary" :underline="false" @click="go('fund')">资金管理 ›</el-link>
              </div>
            </div>
          </template>
          <div v-if="balanceTop.length" class="rank-list">
            <div class="rank-row" v-for="(item, idx) in balanceTop" :key="item.customerId">
              <span class="rank-idx">{{ idx + 1 }}</span>
              <span class="rank-name" :title="item.customerName || '未命名客户'">
                {{ item.customerName || "未命名客户" }}
              </span>
              <span class="rank-channel" :title="item.ownerName || ''">{{ item.ownerName || "-" }}</span>
              <span class="rank-bar"><span class="rank-bar-inner" :style="{ width: balanceBarWidth(item) }"></span></span>
              <span class="rank-amount" :title="fullAmount(item.balance)">{{ shortAmount(item.balance) }}</span>
            </div>
          </div>
          <el-empty v-else :image-size="56" description="暂无客户充值余额" />
          </el-card>
        </div>
      </el-col>

      <el-col :xs="24" :lg="9">
        <div class="col-stack">
          <!-- 快捷入口：与「待我处理」同列，位于右列顶部 -->
          <el-card shadow="never" class="panel">
            <template #header>
              <div class="card-head"><span>快捷入口</span></div>
            </template>
            <div class="quick-list">
              <el-button v-for="q in quickLinks" :key="q.label" @click="go(q.target)">{{ q.label }}</el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="panel">
          <template #header>
            <div class="card-head">
              <span>待我处理</span>
              <span class="head-hint" v-if="todoTotal">共 {{ todoTotal }} 项</span>
            </div>
          </template>
          <template v-if="todoList.length">
            <div class="todo-row" v-for="item in todoList" :key="item.key" @click="go(item.target)">
              <span class="todo-label">{{ item.label }}</span>
              <span class="todo-right">
                <b class="todo-num" :class="{ 'todo-zero': !item.count }">{{ item.count }}</b>
                <span class="todo-unit">{{ item.unit }}</span>
                <span class="todo-arrow">›</span>
              </span>
            </div>
            <p class="todo-tip">数字可点击，进入已按状态筛选的列表</p>
          </template>
          <el-empty v-else :image-size="56" description="当前角色暂无待办项" />
          </el-card>
        </div>
      </el-col>
    </el-row>

    <!-- 趋势：横向需要宽度承载 6 个刻度，独占一行；无数据时用空态覆盖而不是渲染一排 0 -->
    <el-card v-if="canStat" shadow="never" class="panel trend-card">
      <template #header>
        <div class="card-head">
          <span>近 {{ TREND_MONTHS }} 期走势</span>
          <span class="head-hint">应收 / 成本 / 毛利，截止 {{ billCycle }}</span>
        </div>
      </template>
      <div class="trend-wrap">
        <div ref="trendRef" class="trend-chart" :class="{ 'is-hidden': !trendHasData }"></div>
        <el-empty
          v-if="!trendHasData"
          class="trend-empty"
          :image-size="56"
          description="近 6 期暂无用量数据"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup name="Index">
import { getUsageBoard, getUsageTrend } from "@/api/crm/usage"
import { listBill, listBillDiff } from "@/api/crm/bill"
import { listFundAccount, listFundRecharge } from "@/api/crm/fund"
import { listContract } from "@/api/crm/contract"
import { checkPermi } from "@/utils/permission"
import { parseTime } from "@/utils/ruoyi"
import useUserStore from "@/store/modules/user"
// 首页只需要柱状与折线，按需引入避免把整个 echarts 打进首屏包
import * as echarts from "echarts/core"
import { BarChart, LineChart } from "echarts/charts"
import { AxisPointerComponent, GridComponent, LegendComponent, TooltipComponent } from "echarts/components"
import { CanvasRenderer } from "echarts/renderers"

echarts.use([
  BarChart,
  LineChart,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  AxisPointerComponent,
  CanvasRenderer
])

/** 趋势固定 6 期：与「近 6 期」文案一致，也避免用户误以为可无限拉长 */
const TREND_MONTHS = 6

const { proxy } = getCurrentInstance()
const userStore = useUserStore()

const loading = ref(false)
const loadedAt = ref("")
const billCycle = ref(parseTime(new Date(), "{y}-{m}"))
const board = ref({})
const trend = ref([])
const trendRef = ref(null)
/** 客户充值余额榜（后端已按余额倒序 + 过滤零余额，前端不再排序） */
const balanceTop = ref([])
const balanceTopTotal = ref(0)
/** 待办计数按规则 key 存放，规则被角色裁掉时对应数据不会请求 */
const todoCounts = reactive({})

/** 权限判断复用项目已有工具（管理员 *:*:* 直接通过）；无权限的模块不发起请求 */
const canStat = computed(() => checkPermi(["crm:usage:stat"]))
const canBill = computed(() => checkPermi(["crm:bill:list"]))
const canFund = computed(() => checkPermi(["crm:fund:list"]))

/**
 * 待办裁剪规则
 *
 * 裁剪依据是「处理权限」而非「查看权限」：只有查看权限的待办点进去仍是只读的，
 * 留在「待我处理」里只会制造噪音，因此用 audit / diff-handle 这类处理权限作主判据。
 *
 * roles 是兜底白名单：历史授权中可能存在「只勾了查看、漏勾操作」的角色配置，
 * 这类账号如果是财务/商务角色，仍应看到自己该处理的活，不能因为一次菜单勾选遗漏就变空。
 * 新增角色时在此追加 role_key（sys_role.role_key）即可，无需改动其他逻辑。
 */
const TODO_RULES = [
  {
    key: "bill",
    label: "待复核账单",
    shortLabel: "账单",
    unit: "笔",
    target: "billPending",
    viewPermi: "crm:bill:list",
    actionPermi: "crm:bill:audit",
    roles: ["admin", "finance", "caiwu"]
  },
  {
    key: "diff",
    label: "待处理差异",
    shortLabel: "差异",
    unit: "条",
    target: "diffPending",
    viewPermi: "crm:bill:diff",
    actionPermi: "crm:bill:diff-handle",
    roles: ["admin", "finance", "caiwu"]
  },
  {
    key: "contract",
    label: "待审批合同",
    shortLabel: "合同",
    unit: "份",
    target: "contractPending",
    viewPermi: "crm:contract:list",
    actionPermi: "crm:contract:audit",
    roles: ["admin", "business", "sales"]
  },
  {
    key: "fund",
    label: "待审核充值",
    shortLabel: "充值",
    unit: "笔",
    target: "fundPending",
    viewPermi: "crm:fund:list",
    actionPermi: "crm:fund:recharge-audit",
    roles: ["admin", "finance", "caiwu"]
  }
]

const roleKeys = computed(() => (userStore.roles || []).map(item => String(item).toLowerCase()))

function hasRole(roles) {
  return roleKeys.value.some(key => roles.includes(key))
}

/** 当前角色实际可见的待办：先看查看权限，再看处理权限或角色白名单 */
const visibleTodos = computed(() =>
  TODO_RULES.filter(rule => checkPermi([rule.viewPermi]) && (checkPermi([rule.actionPermi]) || hasRole(rule.roles)))
)

const nickName = computed(() => userStore.nickName || userStore.name || "你好")
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return "夜深了"
  if (hour < 11) return "早上好"
  if (hour < 13) return "中午好"
  if (hour < 18) return "下午好"
  return "晚上好"
})

function shortAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fullAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toFixed(8)
}

function rateText(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toFixed(2) + "%"
}

const todoList = computed(() =>
  visibleTodos.value.map(rule => ({ ...rule, count: Number(todoCounts[rule.key] || 0) }))
)

const todoTotal = computed(() => todoList.value.reduce((sum, item) => sum + item.count, 0))

/** 卡片副标题只列当前角色可见的待办，未授权的类别不出现在文案里 */
const todoSummary = computed(() => {
  if (!todoList.value.length) {
    return "当前角色无待办项"
  }
  return todoList.value.map(item => `${item.shortLabel} ${item.count}`).join(" · ")
})

const cards = computed(() => {
  const b = board.value || {}
  return [
    {
      label: "本期应收",
      value: shortAmount(b.totalSellAmount),
      full: fullAmount(b.totalSellAmount),
      sub: canBill.value ? `账单 ${Number(b.billCount || 0)} 张` : "无账单查看权限",
      cls: "c-primary"
    },
    {
      label: "本期渠道成本",
      value: shortAmount(b.totalCostAmount),
      full: fullAmount(b.totalCostAmount),
      sub: `异常占比 ${rateText(b.abnormalRate)}`
    },
    {
      label: "本期毛利",
      value: shortAmount(b.totalGrossProfit),
      full: fullAmount(b.totalGrossProfit),
      sub: `毛利率 ${rateText(b.profitRate)}`,
      cls: "c-success"
    },
    {
      label: "待我处理",
      value: String(todoTotal.value),
      sub: todoSummary.value,
      cls: todoTotal.value > 0 ? "c-danger" : ""
    }
  ]
})

/**
 * 成本 TOP5：后端已按「客户 + 渠道」聚合，这里只做成本倒序取前 5；
 * 占比分母用概览成本总额，与看板卡片保持同一口径
 */
const topCustomers = computed(() => {
  const list = board.value.customerSupplierStats || []
  return list
    .filter(item => Number(item.totalCostAmount) > 0)
    .sort((a, b) => Number(b.totalCostAmount) - Number(a.totalCostAmount))
    .slice(0, 5)
})

const topMax = computed(() => (topCustomers.value.length ? Number(topCustomers.value[0].totalCostAmount) : 0))

function barWidth(item) {
  if (!topMax.value) {
    return "0%"
  }
  const percent = (Number(item.totalCostAmount) / topMax.value) * 100
  return Math.max(percent, 2).toFixed(1) + "%"
}

function shareRate(item) {
  const total = Number((board.value || {}).totalCostAmount || 0)
  if (!total) {
    return "-"
  }
  return ((Number(item.totalCostAmount) / total) * 100).toFixed(2) + "%"
}

/** 余额条形以榜首为满格；数值差距大时也能看出梯队，最小留 2% 保证可见 */
const balanceTopMax = computed(() =>
  balanceTop.value.length ? Number(balanceTop.value[0].balance || 0) : 0
)

function balanceBarWidth(item) {
  if (!balanceTopMax.value) {
    return "0%"
  }
  const percent = (Number(item.balance || 0) / balanceTopMax.value) * 100
  return Math.max(percent, 2).toFixed(1) + "%"
}

const quickLinks = computed(() => {
  const links = [
    { label: "对账看板", target: "reconcile" },
    { label: "用量明细", target: "usage" },
    { label: "对账账单", target: "bill" },
    { label: "客户资金", target: "fund" },
    { label: "差异处理", target: "diff" }
  ]
  // 无资金权限的角色不展示该入口，避免点进去 403
  return links.filter(item => item.target !== "fund" || canFund.value)
})

/**
 * 菜单路径随部署配置可能变化，按路径后缀在已注册路由中反查；
 * 查不到时回退约定路径，避免写死单一前缀导致 404
 */
const ROUTE_MAP = {
  reconcile: { suffix: "/finance/reconcile", fallback: "/crm/finance/reconcile" },
  usage: { suffix: "/finance/usage", fallback: "/crm/finance/usage" },
  bill: { suffix: "/finance/bill", fallback: "/crm/finance/bill" },
  billPending: { suffix: "/finance/bill", fallback: "/crm/finance/bill", query: { billStatus: 1 } },
  diff: { suffix: "/finance/diff", fallback: "/crm/finance/diff" },
  diffPending: { suffix: "/finance/diff", fallback: "/crm/finance/diff", query: { handleStatus: 0 } },
  fund: { suffix: "/finance/fund", fallback: "/crm/finance/fund" },
  fundPending: { suffix: "/finance/fund", fallback: "/crm/finance/fund", query: { tab: "recharge" } },
  contractPending: { suffix: "/contract", fallback: "/crm/contract", query: { statusList: 1 } }
}

function resolvePath(suffix, fallback) {
  const hit = proxy.$router.getRoutes().find(route => route.path.endsWith(suffix))
  return hit ? hit.path : fallback
}

function go(target) {
  const conf = ROUTE_MAP[target]
  if (!conf) {
    return
  }
  proxy.$router.push({ path: resolvePath(conf.suffix, conf.fallback), query: conf.query })
}

/**
 * 各待办类别只取计数：pageSize=1 + res.total，不拉列表数据。
 * 键与 TODO_RULES.key 一一对应，新增待办项时两处同时补。
 */
const TODO_LOADERS = {
  bill: () => listBill({ pageNum: 1, pageSize: 1, params: { statusList: "1" } }),
  diff: () => listBillDiff({ pageNum: 1, pageSize: 1, handleStatus: 0 }),
  contract: () => listContract({ pageNum: 1, pageSize: 1, params: { statusList: "1" } }),
  fund: () => listFundRecharge({ pageNum: 1, pageSize: 1, status: 0 })
}

/** 只请求当前角色可见的待办，被裁掉的类别不发请求（避免无谓的 403 与流量） */
function loadTodos() {
  return Promise.allSettled(
    visibleTodos.value.map(rule =>
      TODO_LOADERS[rule.key]().then(res => {
        todoCounts[rule.key] = res.total || 0
      })
    )
  )
}

function loadAll() {
  loading.value = true
  const tasks = []
  if (canStat.value) {
    tasks.push(
      getUsageBoard(billCycle.value).then(res => {
        board.value = res.data || {}
      }),
      getUsageTrend(billCycle.value, TREND_MONTHS).then(res => {
        trend.value = res.data || []
      })
    )
  }
  /** 余额是实时值，与账期无关；零余额客户由后端 hasBalance=1 过滤，取前 10 条即榜单 */
  if (canFund.value) {
    tasks.push(
      listFundAccount({ pageNum: 1, pageSize: 10, hasBalance: 1 }).then(res => {
        balanceTop.value = res.rows || []
        balanceTopTotal.value = res.total || 0
      })
    )
  }
  tasks.push(loadTodos())
  Promise.allSettled(tasks).finally(() => {
    loading.value = false
    loadedAt.value = parseTime(new Date(), "{h}:{i}")
    nextTick(renderTrend)
  })
}

/** 空态判定：所有账期都没有调用记录时不出图，避免画一条贴地的零线误导读者 */
const trendHasData = computed(() =>
  (trend.value || []).some(item => Number(item.totalCalls || 0) > 0)
)

let trendChart = null

/** 主题色从 CSS 变量读取，浅色/深色主题下坐标轴文字与分割线都不会失效 */
function cssVar(name, fallback) {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name)
  return (value || "").trim() || fallback
}

function axisMoney(value) {
  const num = Number(value || 0)
  if (Math.abs(num) >= 10000) {
    return (num / 10000).toFixed(1) + "万"
  }
  return String(num)
}

function buildTrendOption() {
  const rows = trend.value || []
  const textColor = cssVar("--el-text-color-secondary", "#909399")
  const splitColor = cssVar("--el-border-color-lighter", "#ebeef5")

  return {
    color: ["#409eff", "#e6a23c", "#67c23a"],
    grid: { left: 8, right: 12, top: 38, bottom: 4, containLabel: true },
    legend: {
      right: 0,
      top: 0,
      icon: "roundRect",
      itemWidth: 10,
      itemHeight: 6,
      itemGap: 16,
      textStyle: { color: textColor, fontSize: 12 }
    },
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      formatter: params => {
        const row = rows[params[0].dataIndex] || {}
        const lines = [
          `<div style="font-weight:500;margin-bottom:4px">${row.billCycle} · 调用 ${Number(
            row.totalCalls || 0
          )} 次</div>`
        ]
        params.forEach(item => {
          lines.push(
            `${item.marker}${item.seriesName}<span style="float:right;margin-left:28px;font-variant-numeric:tabular-nums">${shortAmount(
              item.value
            )}</span>`
          )
        })
        lines.push(`毛利率 ${rateText(row.profitRate)} · 异常占比 ${rateText(row.abnormalRate)}`)
        return lines.join("<br/>")
      }
    },
    xAxis: {
      type: "category",
      data: rows.map(item => String(item.billCycle || "").slice(2).replace("-", "/")),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: splitColor } },
      axisLabel: { color: textColor, fontSize: 12 }
    },
    yAxis: {
      type: "value",
      axisLabel: { color: textColor, fontSize: 12, formatter: axisMoney },
      splitLine: { lineStyle: { color: splitColor, type: "dashed" } }
    },
    series: [
      {
        name: "应收",
        type: "bar",
        barMaxWidth: 20,
        itemStyle: { borderRadius: [3, 3, 0, 0] },
        data: rows.map(item => Number(item.totalSellAmount || 0))
      },
      {
        name: "成本",
        type: "bar",
        barMaxWidth: 20,
        itemStyle: { borderRadius: [3, 3, 0, 0] },
        data: rows.map(item => Number(item.totalCostAmount || 0))
      },
      {
        name: "毛利",
        type: "line",
        smooth: true,
        symbolSize: 6,
        lineStyle: { width: 2 },
        data: rows.map(item => Number(item.totalGrossProfit || 0))
      }
    ]
  }
}

function renderTrend() {
  if (!trendRef.value || !trendHasData.value) {
    return
  }
  if (!trendChart) {
    trendChart = echarts.init(trendRef.value)
  }
  // notMerge=true：切换账期后系列点数若变化，旧数据不能残留
  trendChart.setOption(buildTrendOption(), true)
  trendChart.resize()
}

function resizeTrend() {
  if (trendChart) {
    trendChart.resize()
  }
}

onMounted(() => {
  window.addEventListener("resize", resizeTrend)
  // loadAll 在 setup 期即发起，首屏数据可能早于挂载返回，这里补一次渲染
  nextTick(renderTrend)
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeTrend)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})

loadAll()
</script>

<style scoped lang="scss">
.home {
  padding-bottom: 24px;
}

.home-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;

  .greet {
    font-size: 18px;
    font-weight: 500;
    color: var(--el-text-color-primary);
  }

  .head-sub {
    margin-top: 6px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .head-actions {
    display: flex;
    gap: 8px;
  }
}

.stat-card {
  padding: 14px 16px;
  margin-bottom: 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stat-value {
  margin-top: 6px;
  font-size: 20px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.c-primary {
  color: var(--el-color-primary);
}

.c-success {
  color: var(--el-color-success);
}

.c-danger {
  color: var(--el-color-danger);
}

.content-row {
  margin-bottom: 12px;
}

.trend-card {
  margin-bottom: 0;
}

/* 固定高度给 echarts 容器，避免首次渲染时容器为 0 高导致空白 */
.trend-wrap {
  position: relative;
  height: 260px;
}

.trend-chart {
  width: 100%;
  height: 100%;
}

/* 无数据时不销毁容器（销毁后重新 init 会丢失绑定），只隐藏画布 */
.is-hidden {
  visibility: hidden;
}

.trend-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0;
}

/* 列内多卡片纵向堆叠：最后一张撑满剩余高度，避免左右两列底部参差 */
.col-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;

  > .panel:last-child {
    flex: 1;
  }
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
}

.head-hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.head-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rank-list {
  padding-top: 2px;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  font-size: 13px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  &:last-child {
    border-bottom: none;
  }
}

.rank-idx {
  width: 18px;
  flex: none;
  color: var(--el-text-color-placeholder);
  font-variant-numeric: tabular-nums;
}

.rank-name {
  min-width: 100px;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-text-color-primary);
}

.rank-channel {
  min-width: 80px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-text-color-secondary);
}

.rank-bar {
  flex: 1;
  height: 6px;
  min-width: 60px;
  background: var(--el-fill-color);
  border-radius: 3px;
  overflow: hidden;
}

.rank-bar-inner {
  display: block;
  height: 6px;
  background: var(--el-color-primary-light-3);
  border-radius: 3px;
}

.rank-amount {
  width: 108px;
  flex: none;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.rank-rate {
  width: 62px;
  flex: none;
  text-align: right;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}

.todo-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 0;
  font-size: 13px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;

  &:last-of-type {
    border-bottom: none;
  }

  &:hover .todo-label {
    color: var(--el-color-primary);
  }
}

.todo-label {
  color: var(--el-text-color-regular);
}

.todo-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.todo-num {
  font-variant-numeric: tabular-nums;
  color: var(--el-color-danger);
}

.todo-zero {
  color: var(--el-text-color-placeholder);
}

.todo-unit {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.todo-arrow {
  color: var(--el-text-color-placeholder);
}

.todo-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.quick-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
