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
            <label class="info-label">渠道类型：</label>
            <span class="info-value plaintext">
              <el-tag :type="info.supplierType === 2 ? 'warning' : 'success'" size="small" effect="plain">
                {{ supplierTypeLabel(info.supplierType) }}
              </el-tag>
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">负责人：</label>
            <span class="info-value plaintext">
              {{ info.ownerUser }}
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">状态：</label>
            <span class="info-value plaintext">
              <dict-tag :options="crm_status" :value="info.status" />
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
            <label class="info-label">详细地址：</label>
            <span class="info-value plaintext">
              {{ info.detailAddress }}
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">备注：</label>
            <span class="info-value plaintext">
              {{ info.remark }}
            </span>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="mb8">
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
        <el-table-column label="模型所属供应商" min-width="150" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.projectSupplierName || '-' }}</template>
        </el-table-column>
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
      </el-table>
      <h4 class="section-header">渠道账户</h4>
      <el-alert
          v-if="accountsError"
          type="warning"
          :closable="false"
          show-icon
          title="渠道账户加载失败，请稍后重试"
          class="mb8"
      />
      <el-table v-else :data="accounts" border size="small" class="mb8" empty-text="暂无渠道账户">
        <el-table-column label="账户名" min-width="140" prop="accountName" show-overflow-tooltip />
        <el-table-column label="apiKey编号" min-width="130" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.apiKeyNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="apiKey" min-width="210" show-overflow-tooltip>
          <template #default="scope">
            <span class="mono">{{ scope.row.apiKey || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="账户类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.type === 2 ? 'success' : 'info'" size="small" effect="plain">
              {{ accountTypeLabel(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small" effect="plain">
              {{ scope.row.status === 1 ? '启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建者" width="110" align="center">
          <template #default="scope">{{ scope.row.creatorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="165" align="center" prop="createTime" />
      </el-table>
    </div>
  </el-drawer>
</template>

<script setup name="SupplierViewDrawer">
import { getSupplier, getSupplierProjects, getSupplierAccounts } from '@/api/crm/supplier'

const { proxy } = getCurrentInstance()

const visible = ref(false)
const loading = ref(false)
const info = reactive({})

/** 产品关联明细：来自 /crm/supplier/{id}/projects，已含模型型号 projectNo 与模型厂商 projectName */
const projects = ref([])
/** 产品明细接口是否加载失败（失败时不再渲染表格，避免误显示为「无关联」） */
const projectsError = ref(false)

/** 渠道账户列表：来自 /crm/supplier/{id}/accounts，apiKey 已由后端脱敏 */
const accounts = ref([])
/** 账户接口是否加载失败（失败时同样不渲染空表，避免误判为「无账户」） */
const accountsError = ref(false)

/** 账户类型选项：与后端 type 约定一致（1测试账户 2真实账户） */
const accountTypeOptions = [
  { label: '测试账户', value: 1 },
  { label: '真实账户', value: 2 }
]

/** 渠道类型选项：与后端 crm_supplier.supplier_type 约定一致（1自营 2三方） */
const supplierTypeOptions = [
  { label: '自营', value: 1 },
  { label: '三方', value: 2 }
]

/** 渠道类型文案：历史空值按自营展示，与数据库默认值口径一致 */
function supplierTypeLabel(value) {
  const hit = supplierTypeOptions.find(item => item.value === value)
  return hit ? hit.label : '自营'
}

/** 账户类型文案 */
function accountTypeLabel(value) {
  const hit = accountTypeOptions.find(item => item.value === value)
  return hit ? hit.label : '-'
}

/** 数值展示：空值显示 -，有值统一保留两位小数 */
function formatNumber(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const num = Number(value)
  return isNaN(num) ? value : num.toFixed(2)
}

const { crm_deleted, crm_status } = useDict('crm_deleted', 'crm_status')

/** 是否为空值 */
function isEmpty(val) {
  return val === null || val === undefined || val === ''
}

/** 清空详情数据 */
function clearInfo() {
  Object.keys(info).forEach(key => delete info[key])
  projects.value = []
  projectsError.value = false
  accounts.value = []
  accountsError.value = false
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
    const [detailRes, projectRes, accountRes] = await Promise.all([
      getSupplier(id),
      // 明细接口单独处理失败：避免整页报错，也避免把「加载失败」误显示为「无关联」
      getSupplierProjects(id).catch(error => {
        console.error('获取供应商产品明细失败:', error)
        projectsError.value = true
        return null
      }),
      // 账户列表独立容错：账户接口异常不应影响基本信息与产品明细展示
      getSupplierAccounts(id).catch(error => {
        console.error('获取供应商渠道账户失败:', error)
        accountsError.value = true
        return null
      })
    ])
    Object.assign(info, detailRes.data || {})
    projects.value = projectRes && Array.isArray(projectRes.data) ? projectRes.data : []
    accounts.value = accountRes && Array.isArray(accountRes.data) ? accountRes.data : []
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

<style scoped>
.mono {
  font-family: Consolas, Monaco, "Courier New", monospace;
  font-size: 12px;
}
</style>
