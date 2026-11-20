<template>
  <div class="app-container" v-loading="loading">
    <el-page-header @back="handleBack" class="page-header">
      <template #content>
        <span class="page-title">{{ detail.name || "商机详情" }}</span>
        <el-tag class="page-status" :type="statusMeta.type" :effect="statusMeta.plain ? 'plain' : 'dark'">
          {{ detail.statusName || statusMeta.label }}
        </el-tag>
      </template>
      <template #extra>
        <div class="header-actions">
          <el-button
            v-if="detail.mainEditable"
            type="primary"
            plain
            icon="Edit"
            @click="handleEdit"
            v-hasPermi="['crm:business:edit']"
          >编辑</el-button>
          <el-button
            v-for="action in statusActions"
            :key="action.target"
            :type="action.type"
            :icon="action.icon"
            @click="handleAction(action)"
            v-hasPermi="['crm:business:edit']"
          >{{ action.label }}</el-button>
          <el-button
            v-if="detail.convertible"
            type="success"
            icon="DocumentAdd"
            @click="handleConvert"
            v-hasPermi="['crm:business:convert']"
          >转合同</el-button>
          <el-button icon="Back" @click="handleBack">返回</el-button>
        </div>
      </template>
    </el-page-header>

    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="基础信息" name="base">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="商机名称">{{ detail.name || "-" }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detail.ownerUser || "-" }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ detail.customer || "-" }}</el-descriptions-item>
          <el-descriptions-item label="渠道（明细）">{{ detail.supplier || "-" }}</el-descriptions-item>
          <el-descriptions-item label="商机状态">{{ detail.statusName || statusMeta.label }}</el-descriptions-item>
          <el-descriptions-item label="成交时间">{{ detail.dealTime ? parseTime(detail.dealTime) : "-" }}</el-descriptions-item>
          <el-descriptions-item label="意向合同金额">{{ formatAmount(detail.contactAmount) }}</el-descriptions-item>
          <el-descriptions-item label="授信金额">{{ formatAmount(detail.creditAmount) }}</el-descriptions-item>
          <el-descriptions-item label="合同状态" :span="2">
            <el-tag v-if="Number(detail.contactStatus) === 1" type="success" size="small">已签</el-tag>
            <el-tag v-else type="info" size="small" effect="plain">未签</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="中断备注" :span="2">
            <span :class="{ 'text-placeholder': !detail.failRemark }">{{ detail.failRemark || "无" }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            <span :class="{ 'text-placeholder': !detail.remark }">{{ detail.remark || "无" }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detail.creator || "-" }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createTime ? parseTime(detail.createTime) : "-" }}</el-descriptions-item>
          <el-descriptions-item label="更新人">{{ detail.updater || "-" }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detail.updateTime ? parseTime(detail.updateTime) : "-" }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane :label="`意向模型产品（${productRows.length}）`" name="product">
        <el-alert
          v-if="detail.productEditable === false"
          type="warning"
          :closable="false"
          show-icon
          title="商机已提交测试，意向产品清单已锁定"
          class="mb8"
        />
        <el-table :data="productRows" border size="small" empty-text="暂无意向模型产品">
          <el-table-column label="渠道" min-width="140" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.supplierName || "-" }}</template>
          </el-table-column>
          <el-table-column label="渠道账户" min-width="160" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.supplierAccountName || "-" }}</template>
          </el-table-column>
          <el-table-column label="模型型号" min-width="160" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.productNo || scope.row.productName || scope.row.productId }}</template>
          </el-table-column>
          <el-table-column label="模型厂商" min-width="140" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.productName || "-" }}</template>
          </el-table-column>
          <el-table-column label="成本折扣" width="120" align="center">
            <template #default="scope">{{ formatDiscount(scope.row.costDiscount) }}</template>
          </el-table-column>
          <el-table-column label="售卖折扣" width="120" align="center">
            <template #default="scope">{{ formatDiscount(scope.row.sellDiscount) }}</template>
          </el-table-column>
          <el-table-column label="价格明细" min-width="220" show-overflow-tooltip>
            <template #default="scope">
              <span v-if="scope.row.priceDetail">{{ scope.row.priceDetail }}</span>
              <span v-else class="text-placeholder">未填写</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`关联合同（${contractRows.length}）`" name="contract">
        <el-table :data="contractRows" border size="small" empty-text="尚未生成合同">
          <el-table-column label="合同编号" min-width="180">
            <template #default="scope">
              <el-link type="primary" :underline="false" @click="handleViewContract(scope.row)">{{ scope.row.contactNo }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="合同名称" min-width="200" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.contractName || "-" }}</template>
          </el-table-column>
          <el-table-column label="合同状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="contractTagType(scope.row.status)" size="small">{{ scope.row.statusName || "-" }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="签约时间" width="170" align="center">
            <template #default="scope">{{ scope.row.signTime ? parseTime(scope.row.signTime) : "-" }}</template>
          </el-table-column>
          <el-table-column label="到期时间" width="170" align="center">
            <template #default="scope">{{ scope.row.expireTime ? parseTime(scope.row.expireTime) : "-" }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="handleViewContract(scope.row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`操作日志（${logRows.length}）`" name="log">
        <el-timeline v-if="logRows.length">
          <el-timeline-item
            v-for="item in logRows"
            :key="item.id"
            :timestamp="item.createTime ? parseTime(item.createTime) : ''"
            placement="top"
            :type="logTagType(item.action)"
          >
            <div class="log-item">
              <span class="log-action">{{ item.actionName || item.action }}</span>
              <span class="log-operator">{{ item.operatorName || "系统" }}</span>
            </div>
            <div class="log-content">{{ item.content || "-" }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无操作日志" />
      </el-tab-pane>
    </el-tabs>

    <!-- 中断弹窗 -->
    <el-dialog title="中断商机" v-model="interruptOpen" width="460px" append-to-body>
      <el-form ref="interruptFormRef" :model="interruptForm" :rules="interruptRules" label-width="90px">
        <el-form-item label="商机名称">
          <span>{{ detail.name || "-" }}</span>
        </el-form-item>
        <el-form-item label="失败备注" prop="failRemark">
          <el-input
            v-model="interruptForm.failRemark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请填写中断原因，中断后不可恢复"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="danger" @click="submitInterrupt">确 认 中 断</el-button>
          <el-button @click="interruptOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="BusinessDetail">
import { getBusiness, changeBusinessStatus, convertContract } from "@/api/crm/business"

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const STATUS = { INIT: 0, FOLLOWING: 1, SUBMIT_TEST: 2, TESTING: 3, DEAL: 4, PAUSED: 5, INTERRUPTED: 6 }
const STATUS_META = {
  [STATUS.INIT]: { label: "初始化", type: "info", plain: true },
  [STATUS.FOLLOWING]: { label: "跟进中", type: "primary", plain: false },
  [STATUS.SUBMIT_TEST]: { label: "提交测试", type: "primary", plain: true },
  [STATUS.TESTING]: { label: "测试中", type: "warning", plain: false },
  [STATUS.DEAL]: { label: "成交", type: "success", plain: false },
  [STATUS.PAUSED]: { label: "暂停", type: "warning", plain: true },
  [STATUS.INTERRUPTED]: { label: "中断", type: "danger", plain: false }
}

/** 目标状态按钮样式，文案按当前状态上下文生成 */
const TARGET_META = {
  [STATUS.FOLLOWING]: { type: "primary", icon: "Phone" },
  [STATUS.SUBMIT_TEST]: { type: "warning", icon: "Promotion" },
  [STATUS.TESTING]: { type: "warning", icon: "Promotion" },
  [STATUS.DEAL]: { type: "success", icon: "CircleCheck" },
  [STATUS.PAUSED]: { type: "info", icon: "VideoPause" },
  [STATUS.INTERRUPTED]: { type: "danger", icon: "CircleClose" }
}

const loading = ref(false)
const activeTab = ref("base")
const detail = reactive({})

const listPath = computed(() => {
  const base = route.path.replace(/\/+$/, "")
  return /\/detail$/.test(base) ? base.replace(/\/detail$/, "") : "/crm/business"
})

const contractBasePath = computed(() => {
  const base = listPath.value
  return /\/business$/.test(base) ? base.replace(/\/business$/, "/contract") : "/crm/contract"
})

const statusMeta = computed(() => STATUS_META[Number(detail.busStatus)] || STATUS_META[STATUS.INIT])
const productRows = computed(() => detail.businessProducts || [])
const contractRows = computed(() => detail.contracts || [])
const logRows = computed(() => detail.logs || [])

/** 状态流转按钮：以后端返回的 allowedStatuses 为准，避免前后端规则漂移 */
const statusActions = computed(() => {
  const current = Number(detail.busStatus)
  return (detail.allowedStatuses || []).map(item => {
    const target = Number(item.code)
    const meta = TARGET_META[target] || { type: "primary", icon: "" }
    return {
      target,
      label: actionLabel(current, target, item.info),
      type: meta.type,
      icon: meta.icon
    }
  })
})

function actionLabel(current, target, fallback) {
  if (target === STATUS.FOLLOWING) {
    if (current === STATUS.INIT) {
      return "开始跟进"
    }
    if (current === STATUS.PAUSED) {
      return "恢复跟进"
    }
    return "退回跟进"
  }
  if (target === STATUS.DEAL) {
    return "成交"
  }
  return fallback || "流转"
}

function contractTagType(status) {
  const map = { 0: "info", 1: "primary", 2: "success", 3: "warning", 4: "danger", 5: "info" }
  return map[Number(status)] || "info"
}

function logTagType(action) {
  const map = {
    CREATE: "primary",
    UPDATE: "primary",
    STATUS: "warning",
    PRODUCT: "info",
    CONVERT: "success",
    SETTLE: "success",
    CONTACT: "success",
    DELETE: "danger"
  }
  return map[action] || "info"
}

function formatAmount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  const num = Number(value)
  return isNaN(num) ? String(value) : num.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDiscount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  const num = Number(value)
  return isNaN(num) ? String(value) : num.toFixed(2)
}

function getDetail() {
  const id = route.query.id
  if (!id) {
    proxy.$modal.msgError("缺少商机ID")
    handleBack()
    return
  }
  loading.value = true
  getBusiness(id).then(res => {
    Object.assign(detail, res.data || {})
  }).catch(() => {
    handleBack()
  }).finally(() => {
    loading.value = false
  })
}

function handleEdit() {
  router.push({ path: listPath.value + "/edit", query: { id: detail.id } })
}

function handleBack() {
  router.push({ path: listPath.value })
}

function handleViewContract(row) {
  router.push({ path: contractBasePath.value + "/detail", query: { id: row.id } })
}

/** 状态流转：中断需填写备注，成交需二次确认，其余直接确认 */
function handleAction(action) {
  if (action.target === STATUS.INTERRUPTED) {
    interruptForm.failRemark = ""
    interruptOpen.value = true
    nextTick(() => interruptFormRef.value && interruptFormRef.value.clearValidate())
    return
  }
  const tip = action.target === STATUS.DEAL
    ? `是否确认将商机「${detail.name}」标记为成交？成交后商机基础信息与意向产品清单将被锁定。`
    : `是否确认将商机「${detail.name}」变更为「${action.label}」？`
  proxy.$modal.confirm(tip).then(() => {
    return changeBusinessStatus({ id: detail.id, busStatus: action.target })
  }).then(() => {
    proxy.$modal.msgSuccess("状态变更成功")
    getDetail()
  }).catch(() => {})
}

const interruptOpen = ref(false)
const interruptFormRef = ref()
const interruptForm = reactive({ failRemark: "" })
const interruptRules = {
  failRemark: [
    { required: true, message: "中断备注不能为空", trigger: "blur" },
    { max: 255, message: "中断备注不能超过 255 个字符", trigger: "blur" }
  ]
}

function submitInterrupt() {
  interruptFormRef.value.validate(valid => {
    if (!valid) {
      return
    }
    changeBusinessStatus({
      id: detail.id,
      busStatus: STATUS.INTERRUPTED,
      failRemark: interruptForm.failRemark.trim()
    }).then(() => {
      proxy.$modal.msgSuccess("商机已中断")
      interruptOpen.value = false
      getDetail()
    })
  })
}

function handleConvert() {
  proxy.$modal.confirm("将根据当前商机意向清单生成草稿合同，确认继续？").then(() => {
    return convertContract(detail.id)
  }).then(res => {
    proxy.$modal.msgSuccess("已生成草稿合同")
    if (res.data) {
      router.push({ path: contractBasePath.value + "/edit", query: { id: res.data } })
    } else {
      getDetail()
    }
  }).catch(() => {})
}

getDetail()
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.page-status {
  margin-left: 10px;
  vertical-align: middle;
}
.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
.detail-tabs {
  margin-top: 4px;
}
.log-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.log-action {
  font-weight: 600;
}
.log-operator {
  font-size: 12px;
  color: #909399;
}
.log-content {
  margin-top: 4px;
  font-size: 13px;
  color: #606266;
}
.text-placeholder {
  color: #c0c4cc;
}
</style>
