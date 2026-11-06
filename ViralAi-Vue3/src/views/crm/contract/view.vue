<template>
  <div class="app-container" v-loading="loading">
    <el-page-header @back="handleBack" content="合同详情" class="page-header" />

    <el-card shadow="never" class="section-card">
      <div class="detail-header">
        <div class="detail-title">
          <span class="contract-no">{{ detail.contactNo }}</span>
          <el-tag :type="statusTagType" size="small">{{ detail.statusName || "草稿" }}</el-tag>
          <span class="contract-name">{{ detail.contractName }}</span>
        </div>
        <div class="detail-actions">
          <el-button
            v-if="isDraft"
            type="primary"
            plain
            icon="Edit"
            @click="handleEdit"
            v-hasPermi="['crm:contract:edit']"
          >编辑</el-button>
          <el-button
            v-if="isDraft"
            type="success"
            plain
            icon="Promotion"
            @click="handleSubmit"
            v-hasPermi="['crm:contract:submit']"
          >提交审批</el-button>
          <el-button
            v-if="isAuditing"
            type="warning"
            plain
            icon="Stamp"
            @click="openAudit"
            v-hasPermi="['crm:contract:audit']"
          >审批</el-button>
          <el-button
            v-if="isAuditing"
            type="info"
            plain
            icon="RefreshLeft"
            @click="handleWithdraw"
            v-hasPermi="['crm:contract:submit']"
          >撤回审批</el-button>
          <el-button
            v-if="canCancel"
            type="danger"
            plain
            icon="CircleClose"
            @click="openCancel"
            v-hasPermi="['crm:contract:cancel']"
          >作废</el-button>
          <el-button
            v-if="canArchive"
            type="info"
            plain
            icon="FolderChecked"
            @click="handleArchive"
            v-hasPermi="['crm:contract:archive']"
          >归档</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="section-card">
      <el-tabs v-model="activeTab">
        <!-- 基础信息 -->
        <el-tab-pane label="基础信息" name="base">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="合同编号">{{ detail.contactNo || "-" }}</el-descriptions-item>
            <el-descriptions-item label="合同名称">{{ detail.contractName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="渠道（明细）">
              {{ detail.supplierName || "-" }}
            </el-descriptions-item>
            <el-descriptions-item label="客户">{{ detail.consumerName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="商机">{{ detail.busName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="签约主体">{{ detail.contractEntity || "-" }}</el-descriptions-item>
            <el-descriptions-item label="签约时间">{{ detail.signTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="到期时间">{{ detail.expireTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="审批时间">{{ detail.auditTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="审批意见">{{ detail.auditOpinion || "-" }}</el-descriptions-item>
            <el-descriptions-item label="作废时间">{{ detail.cancelTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="作废原因">{{ detail.cancelReason || "-" }}</el-descriptions-item>
            <el-descriptions-item label="归档时间">{{ detail.archiveTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ detail.creatorName || "-" }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.createTime || "-" }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.remark || "-" }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="sub-title">状态时间线</h4>
          <el-timeline v-if="timeline.length">
            <el-timeline-item
              v-for="item in timeline"
              :key="item.id"
              :timestamp="item.createTime"
              :type="item.timelineType"
              placement="top"
            >
              <div class="timeline-title">{{ item.actionName }}</div>
              <div class="timeline-content">{{ item.content || "-" }}</div>
              <div class="timeline-operator">操作人：{{ item.operatorName || "-" }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无状态变更记录" :image-size="60" />
        </el-tab-pane>

        <!-- 模型明细 -->
        <el-tab-pane label="模型明细" name="products">
          <el-table v-if="products.length" :data="products" border size="small">
            <el-table-column label="渠道" prop="supplierName" min-width="140" show-overflow-tooltip />
            <el-table-column label="渠道账户" prop="supplierAccountName" min-width="160" show-overflow-tooltip />
            <el-table-column label="模型厂商" prop="productName" min-width="140" />
            <el-table-column label="模型型号" prop="productNo" min-width="160" />
            <el-table-column label="类型" align="center" width="110">
              <template #default="scope">
                <span>{{ productTypeLabel(scope.row.productType) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="成本折扣" align="center" width="110">
              <template #default="scope">{{ formatDiscount(scope.row.costDiscount) }}</template>
            </el-table-column>
            <el-table-column label="售卖折扣" align="center" width="110">
              <template #default="scope">{{ formatDiscount(scope.row.sellDiscount) }}</template>
            </el-table-column>
            <el-table-column label="价格明细" min-width="260">
              <template #default="scope">
                <span class="price-json">{{ scope.row.priceDetail || "-" }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无模型明细" :image-size="60" />
        </el-tab-pane>

        <!-- 审批记录 -->
        <el-tab-pane label="审批记录" name="audit">
          <el-table v-if="auditRecords.length" :data="auditRecords" border size="small">
            <el-table-column label="审批人" prop="operatorName" width="140" />
            <el-table-column label="审批时间" prop="createTime" width="180" />
            <el-table-column label="审批结果" width="110" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.action === 'AUDIT_PASS' ? 'success' : 'danger'" size="small">
                  {{ scope.row.action === "AUDIT_PASS" ? "通过" : "驳回" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审批意见" prop="content" min-width="240" show-overflow-tooltip />
          </el-table>
          <el-empty v-else description="暂无审批记录" :image-size="60" />
        </el-tab-pane>

        <!-- 操作日志 -->
        <el-tab-pane label="操作日志" name="logs">
          <el-table v-if="logs.length" :data="logs" border size="small">
            <el-table-column label="操作" prop="actionName" width="140" />
            <el-table-column label="操作人" prop="operatorName" width="140" />
            <el-table-column label="操作时间" prop="createTime" width="180" />
            <el-table-column label="状态变化" width="180">
              <template #default="scope">
                <span>{{ statusLabel(scope.row.fromStatus) }} → {{ statusLabel(scope.row.toStatus) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="内容" prop="content" min-width="260" show-overflow-tooltip />
          </el-table>
          <el-empty v-else description="暂无操作日志" :image-size="60" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 审批弹窗 -->
    <el-dialog title="合同审批" v-model="auditOpen" width="520px" append-to-body>
      <el-form :model="auditForm" :rules="auditRules" ref="auditRef" label-width="90px">
        <el-form-item label="审批结果" prop="auditResult">
          <el-radio-group v-model="auditForm.auditResult">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="auditOpinion">
          <el-input
            v-model="auditForm.auditOpinion"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="通过时可不填；驳回时必须填写驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAuditForm" :loading="auditSubmitting">确 定</el-button>
          <el-button @click="auditOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 作废弹窗 -->
    <el-dialog title="合同作废" v-model="cancelOpen" width="520px" append-to-body>
      <el-form :model="cancelForm" :rules="cancelRules" ref="cancelRef" label-width="90px">
        <el-form-item label="作废原因" prop="cancelReason">
          <el-input
            v-model="cancelForm.cancelReason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入作废原因（必填），作废后不可恢复签约"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="danger" @click="submitCancelForm" :loading="cancelSubmitting">确认作废</el-button>
          <el-button @click="cancelOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ContractView">
import { getContract, submitAudit, withdrawAudit, auditContract, cancelContract, archiveContract } from "@/api/crm/contract"

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

/** 合同列表路由前缀：/xxx/contract/detail → /xxx/contract */
const contractBasePath = computed(() => route.path.replace(/\/(edit|detail)\/?$/, "") || "/crm/contract")

const loading = ref(false)
const activeTab = ref("base")
const detail = ref({})
const products = ref([])
const logs = ref([])
const auditRecords = ref([])

const STATUS_LABELS = {
  0: "草稿",
  1: "审批中",
  2: "已签约",
  3: "已到期",
  4: "已作废",
  5: "已归档"
}

const STATUS_TAG_TYPES = {
  0: "info",
  1: "primary",
  2: "success",
  3: "warning",
  4: "danger",
  5: "info"
}

const PRODUCT_TYPE_LABELS = {
  1: "文本",
  2: "多模态",
  3: "图片处理",
  4: "音频处理",
  5: "视频处理"
}

const TIMELINE_TYPES = {
  CREATE: "primary",
  SUBMIT: "primary",
  AUDIT_PASS: "success",
  AUDIT_REJECT: "danger",
  CANCEL: "danger",
  ARCHIVE: "info",
  EXPIRE: "warning"
}

const statusCode = computed(() => Number(detail.value.status))
const isDraft = computed(() => statusCode.value === 0)
const isAuditing = computed(() => statusCode.value === 1)
const canCancel = computed(() => statusCode.value === 2 || statusCode.value === 3)
const canArchive = computed(() => [2, 3, 4].includes(statusCode.value))
const statusTagType = computed(() => STATUS_TAG_TYPES[statusCode.value] || "info")

function statusLabel(code) {
  if (code === null || code === undefined) {
    return "-"
  }
  return STATUS_LABELS[Number(code)] || "-"
}

function productTypeLabel(type) {
  return PRODUCT_TYPE_LABELS[Number(type)] || "-"
}

function formatDiscount(value) {
  if (value === null || value === undefined || value === "") {
    return "-"
  }
  return Number(value).toFixed(2)
}

/** 时间线：操作日志按时间正序展示 */
const timeline = computed(() =>
  [...logs.value]
    .sort((a, b) => Number(a.id) - Number(b.id))
    .map(item => ({ ...item, timelineType: TIMELINE_TYPES[item.action] || "primary" }))
)

function loadDetail() {
  const id = route.query.id
  if (!id) {
    proxy.$modal.msgError("缺少合同ID")
    return
  }
  loading.value = true
  getContract(id)
    .then(res => {
      const data = res.data || {}
      detail.value = data
      products.value = data.products || []
      logs.value = data.logs || []
      auditRecords.value = data.auditRecords || []
    })
    .finally(() => {
      loading.value = false
    })
}

function handleBack() {
  router.push({ path: contractBasePath.value })
}

function handleEdit() {
  router.push({ path: contractBasePath.value + "/edit", query: { id: detail.value.id } })
}

function handleSubmit() {
  proxy.$modal.confirm(`确认提交合同【${detail.value.contactNo}】进入审批流程？提交后不可编辑。`).then(() => {
    return submitAudit(detail.value.id)
  }).then(() => {
    proxy.$modal.msgSuccess("提交审批成功")
    loadDetail()
  }).catch(() => {})
}

/** 撤回审批 */
function handleWithdraw() {
  proxy.$modal.confirm(`确认撤回合同【${detail.value.contactNo}】的审批申请？撤回后合同退回草稿。`).then(() => {
    return withdrawAudit(detail.value.id)
  }).then(() => {
    proxy.$modal.msgSuccess("已撤回，合同退回草稿")
    loadDetail()
  }).catch(() => {})
}

/** 审批 */
const auditOpen = ref(false)
const auditSubmitting = ref(false)
const auditRef = ref()
const auditForm = ref({ contractId: undefined, auditResult: 1, auditOpinion: "" })

const auditRules = {
  auditResult: [{ required: true, message: "请选择审批结果", trigger: "change" }],
  auditOpinion: [
    {
      validator: (rule, value, callback) => {
        if (Number(auditForm.value.auditResult) === 2 && !String(value || "").trim()) {
          callback(new Error("驳回时必须填写审批意见"))
          return
        }
        callback()
      },
      trigger: "blur"
    }
  ]
}

function openAudit() {
  auditForm.value = { contractId: detail.value.id, auditResult: 1, auditOpinion: "" }
  auditOpen.value = true
  nextTick(() => auditRef.value && auditRef.value.clearValidate())
}

function submitAuditForm() {
  auditRef.value.validate(valid => {
    if (!valid) {
      return
    }
    auditSubmitting.value = true
    auditContract({ ...auditForm.value, auditOpinion: String(auditForm.value.auditOpinion || "").trim() })
      .then(() => {
        proxy.$modal.msgSuccess(Number(auditForm.value.auditResult) === 1 ? "审批通过，合同已签约" : "已驳回，合同退回草稿")
        auditOpen.value = false
        loadDetail()
      })
      .finally(() => {
        auditSubmitting.value = false
      })
  })
}

/** 作废 */
const cancelOpen = ref(false)
const cancelSubmitting = ref(false)
const cancelRef = ref()
const cancelForm = ref({ contractId: undefined, cancelReason: "" })

const cancelRules = {
  cancelReason: [{ required: true, message: "作废原因不能为空", trigger: "blur" }]
}

function openCancel() {
  cancelForm.value = { contractId: detail.value.id, cancelReason: "" }
  cancelOpen.value = true
  nextTick(() => cancelRef.value && cancelRef.value.clearValidate())
}

function submitCancelForm() {
  cancelRef.value.validate(valid => {
    if (!valid) {
      return
    }
    cancelSubmitting.value = true
    cancelContract({
      contractId: detail.value.id,
      cancelReason: String(cancelForm.value.cancelReason || "").trim()
    }).then(() => {
      proxy.$modal.msgSuccess("合同已作废")
      cancelOpen.value = false
      loadDetail()
    }).finally(() => {
      cancelSubmitting.value = false
    })
  })
}

function handleArchive() {
  proxy.$modal.confirm(`归档后合同【${detail.value.contactNo}】不可再编辑、作废或恢复，确认归档？`).then(() => {
    return archiveContract(detail.value.id)
  }).then(() => {
    proxy.$modal.msgSuccess("归档成功")
    loadDetail()
  }).catch(() => {})
}

loadDetail()
</script>

<style scoped>
.page-header {
  margin-bottom: 12px;
}

.section-card {
  margin-bottom: 14px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.contract-no {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.contract-name {
  color: var(--el-text-color-regular);
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.sub-title {
  margin: 22px 0 12px;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.timeline-title {
  font-weight: 600;
}

.timeline-content {
  margin-top: 2px;
  color: var(--el-text-color-regular);
}

.timeline-operator {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.price-json {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
  word-break: break-all;
}
</style>
