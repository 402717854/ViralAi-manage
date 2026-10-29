<template>
  <el-drawer title="CRM 供应商详情" v-model="visible" direction="rtl" size="70%" append-to-body :before-close="handleClose" class="detail-drawer">
    <div v-loading="loading" class="drawer-content">
      <h4 class="section-header">基本信息</h4>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">供应商名称：</label>
            <span class="info-value plaintext">
              {{ info.name }}
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
            <label class="info-label">手机：</label>
            <span class="info-value plaintext">
              {{ info.mobile }}
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">邮箱：</label>
            <span class="info-value plaintext">
              {{ info.email }}
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">状态：</label>
            <span class="info-value plaintext">
              <dict-tag :options="crm_status" :value="info.status" />
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">详细地址：</label>
            <span class="info-value plaintext">
              {{ info.detailAddress }}
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
            <label class="info-label">是否删除：</label>
            <span class="info-value plaintext">
              <dict-tag :options="crm_deleted" :value="info.deleted" />
            </span>
          </div>
        </el-col>
      </el-row>
      <h4 class="section-header">产品明细</h4>
      <el-alert
          v-if="projectsError"
          type="warning"
          :closable="false"
          show-icon
          title="产品明细加载失败，请稍后重试"
          class="mb8"
      />
      <el-table v-else :data="projects" border size="small" class="mb8" empty-text="暂无产品明细">
        <el-table-column label="模型型号" min-width="170" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.projectNo">{{ scope.row.projectNo }}</span>
            <el-tag v-else type="danger" size="small" effect="plain">
              模型已不存在（编号 {{ scope.row.projectId }}）
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模型厂商" min-width="120" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.projectName || '-' }}</template>
        </el-table-column>
        <el-table-column label="最小成本折扣" width="105" align="center">
          <template #default="scope">{{ formatNumber(scope.row.minCostDiscount) }}</template>
        </el-table-column>
        <el-table-column label="最大成本折扣" width="105" align="center">
          <template #default="scope">{{ formatNumber(scope.row.maxCostDiscount) }}</template>
        </el-table-column>
        <el-table-column label="最小售价折扣" width="105" align="center">
          <template #default="scope">{{ formatNumber(scope.row.minSellDiscount) }}</template>
        </el-table-column>
        <el-table-column label="最大售价折扣" width="105" align="center">
          <template #default="scope">{{ formatNumber(scope.row.maxSellDiscount) }}</template>
        </el-table-column>
        <el-table-column label="最小代理折扣" width="105" align="center">
          <template #default="scope">{{ formatNumber(scope.row.minAgentDiscount) }}</template>
        </el-table-column>
        <el-table-column label="最大代理折扣" width="105" align="center">
          <template #default="scope">{{ formatNumber(scope.row.maxAgentDiscount) }}</template>
        </el-table-column>
        <el-table-column label="预计日消耗金额" width="125" align="center">
          <template #default="scope">{{ formatAmount(scope.row.dailyUsageAmount) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>
</template>

<script setup name="SupplierViewDrawer">
import { getSupplier, getSupplierProjects } from '@/api/crm/supplier'

const { proxy } = getCurrentInstance()

const visible = ref(false)
const loading = ref(false)
const info = reactive({})

/** 产品关联明细：来自 /crm/supplier/{id}/projects，已含模型型号 projectNo 与模型厂商 projectName */
const projects = ref([])
/** 产品明细接口是否加载失败（失败时不再渲染表格，避免误显示为「无关联」） */
const projectsError = ref(false)

/** 数值展示：空值显示 -，有值统一保留两位小数 */
function formatNumber(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const num = Number(value)
  return isNaN(num) ? value : num.toFixed(2)
}

/** 金额展示：空值显示 -，有值保留两位小数并加千分位 */
function formatAmount(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const num = Number(value)
  if (isNaN(num)) {
    return value
  }
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const { crm_status, crm_deleted } = useDict('crm_deleted', 'crm_status')

/** 是否为空值 */
function isEmpty(val) {
  return val === null || val === undefined || val === ''
}

/** 清空详情数据 */
function clearInfo() {
  Object.keys(info).forEach(key => delete info[key])
  projects.value = []
  projectsError.value = false
}

/** 打开详情抽屉：基本信息与产品明细并发加载 */
async function open(id) {
  if (isEmpty(id)) {
    proxy.$modal.msgWarning('缺少供应商编号，无法加载详情')
    return
  }
  visible.value = true
  loading.value = true
  clearInfo()
  try {
    const [detailRes, projectRes] = await Promise.all([
      getSupplier(id),
      // 明细接口单独处理失败：避免整页报错，也避免把「加载失败」误显示为「无关联」
      getSupplierProjects(id).catch(error => {
        console.error('获取供应商产品明细失败:', error)
        projectsError.value = true
        return null
      })
    ])
    Object.assign(info, detailRes.data || {})
    projects.value = projectRes && Array.isArray(projectRes.data) ? projectRes.data : []
  } catch (error) {
    console.error('获取CRM 供应商信息失败:', error)
  } finally {
    loading.value = false
  }
}

/** 关闭抽屉（before-close 需调用 done 才会真正关闭） */
function handleClose(done) {
  loading.value = false
  if (typeof done === 'function') {
    done()
  } else {
    visible.value = false
  }
  clearInfo()
}

defineExpose({ open })
</script>
