<template>
  <el-drawer title="CRM 商机详情" v-model="visible" direction="rtl" size="60%" append-to-body :before-close="handleClose" class="detail-drawer">
    <div v-loading="loading" class="drawer-content">
      <h4 class="section-header">基本信息</h4>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">商机名称：</label>
            <span class="info-value plaintext">
              {{ info.name }}
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">客户编号：</label>
            <span class="info-value plaintext">
              {{ info.customerId }}
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">渠道编号：</label>
            <span class="info-value plaintext">
              {{ info.supplierId }}
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">负责人：</label>
            <span class="info-value plaintext">
              {{ info.ownerUser }}
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">商机状态：</label>
            <span class="info-value plaintext">
              <dict-tag :options="crm_bus_status" :value="info.busStatus" />
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">成交日期：</label>
            <span class="info-value plaintext">
              {{ parseTime(info.dealTime, '{y}-{m}-{d}') }}
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">合同状态：</label>
            <span class="info-value plaintext">
              <dict-tag :options="crm_bus_contact" :value="info.contactStatus" />
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">结算状态：</label>
            <span class="info-value plaintext">
              <dict-tag :options="crm_bus_settle" :value="info.settleStatus" />
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">合同金额：</label>
            <span class="info-value plaintext">
              {{ info.contactAmount }}
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">授信金额：</label>
            <span class="info-value plaintext">
              {{ info.creditAmount }}
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">备注：</label>
            <span class="info-value plaintext">
              {{ info.remark }}
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">中断备注：</label>
            <span class="info-value plaintext">
              {{ info.failRemark }}
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">创建人：</label>
            <span class="info-value plaintext">
              {{ info.creator }}
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">更新人：</label>
            <span class="info-value plaintext">
              {{ info.updater }}
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">逻辑删除：</label>
            <span class="info-value plaintext">
              <dict-tag :options="crm_deleted" :value="info.deleted" />
            </span>
          </div>
        </el-col>
      </el-row>
      <h4 class="section-header">模型产品</h4>
      <div v-if="projectsError" class="project-tip">模型名称加载失败，以下仅显示模型编号</div>
      <el-table :data="productRows" border size="small" class="mb8" empty-text="暂无模型产品">
        <el-table-column label="模型" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.modelName">{{ scope.row.modelName }}</span>
            <el-tag v-else-if="scope.row.modelState === 'missing'" type="danger" size="small" effect="plain">
              模型已不存在（编号 {{ scope.row.productId }}）
            </el-tag>
            <el-tag v-else type="info" size="small" effect="plain">
              编号 {{ scope.row.productId ?? "-" }}
              <template v-if="scope.row.modelState === 'unbound'">（不在该供应商产品中）</template>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成本折扣" width="120" align="center">
          <template #default="scope">{{ formatDiscount(scope.row.costDiscount) }}</template>
        </el-table-column>
        <el-table-column label="售卖折扣" width="120" align="center">
          <template #default="scope">{{ formatDiscount(scope.row.sellDiscount) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>
</template>

<script setup name="BusinessViewDrawer">
import { getBusiness } from '@/api/crm/business'
import { getSupplierProjects } from '@/api/crm/supplier'

const { crm_deleted, crm_bus_status, crm_bus_contact, crm_bus_settle } = useDict('crm_deleted', 'crm_bus_status', 'crm_bus_contact', 'crm_bus_settle')

const visible = ref(false)
const loading = ref(false)
const info = reactive({})

/** 该商机所属供应商的产品配置：来源 /crm/supplier/{id}/projects，用于把 productId 还原为型号 */
const supplierProjects = ref([])
/** 供应商产品是否已加载成功：未成功时不臆断「不在该供应商产品中」 */
const projectsLoaded = ref(false)
const projectsError = ref(false)

/** 加载供应商产品配置；未传供应商时直接置空 */
async function loadSupplierProjects(supplierId) {
  supplierProjects.value = []
  projectsLoaded.value = false
  projectsError.value = false
  if (supplierId === null || supplierId === undefined || supplierId === '') {
    return
  }
  try {
    const res = await getSupplierProjects(supplierId)
    supplierProjects.value = Array.isArray(res.data) ? res.data : []
    projectsLoaded.value = true
  } catch (error) {
    // 失败不阻塞详情：型号列退化为编号，并在表格上方提示，避免误导成「模型不存在」
    console.error('获取供应商产品明细失败:', error)
    projectsError.value = true
  }
}

/** 模型产品明细（只读展示）：型号取自供应商产品，缺失时给出明确原因 */
const productRows = computed(() => {
  const list = Array.isArray(info.businessProducts) ? info.businessProducts : []
  const projects = supplierProjects.value
  return list.map(row => {
    const matched = projects.find(item => String(item.projectId) === String(row.productId))
    const projectNo = matched ? (matched.projectNo || matched.projectName || null) : null
    let modelState = ''
    if (!projectNo && projectsLoaded.value) {
      // 关联仍在但型号为空 → 模型已被删除；完全没匹配到 → 该供应商已解绑此模型
      modelState = matched ? 'missing' : 'unbound'
    }
    return {
      ...row,
      modelName: projectNo || row.modelName || null,
      modelState
    }
  })
})

/** 折扣空值统一显示为 - */
function formatDiscount(value) {
  return value === null || value === undefined || value === "" ? "-" : value
}

const open = async (id) => {
  clearInfo()
  visible.value = true
  loading.value = true
  try {
    const res = await getBusiness(id)
    Object.assign(info, res.data || {})
    // 拿到 supplierId 后再取该供应商的产品配置，用于还原明细中的型号
    await loadSupplierProjects(info.supplierId)
  } catch (error) {
    console.error('获取CRM 商机信息失败:', error)
  } finally {
    loading.value = false
  }
}

/** 清空详情数据（含模型映射状态，避免串到下一个商机） */
function clearInfo() {
  Object.keys(info).forEach(key => delete info[key])
  supplierProjects.value = []
  projectsLoaded.value = false
  projectsError.value = false
}

function handleClose() {
  visible.value = false
  clearInfo()
}

defineExpose({ open })
</script>

<style scoped>
.project-tip {
  margin-bottom: 6px;
  font-size: 12px;
  line-height: 18px;
  color: #e6a23c;
}
</style>
