<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="供应商" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入供应商名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机" prop="mobile">
        <el-input
          v-model="queryParams.mobile"
          placeholder="请输入手机"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input
          v-model="queryParams.email"
          placeholder="请输入邮箱"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['crm:supplier:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['crm:supplier:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['crm:supplier:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['crm:supplier:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="supplierList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="供应商名称" align="center" prop="name" fixed="left"/>
      <el-table-column label="负责人" align="center" prop="ownerUser" fixed="left"/>
      <el-table-column label="状态" align="center" prop="status" width="100" fixed="left">
        <template #default="scope">
          <dict-tag :options="crm_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="手机" align="center" prop="mobile" />
      <el-table-column label="邮箱" align="center" prop="email" />
      <el-table-column label="地区" align="center" prop="detailAddress" />
      <el-table-column label="创建者" align="center" prop="creator" />
      <el-table-column label="更新者" align="center" prop="updater" />
      <el-table-column label="是否删除" align="center" prop="deleted">
        <template #default="scope">
          <dict-tag :options="crm_deleted" :value="scope.row.deleted" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleViewData(scope.row)" v-hasPermi="['crm:supplier:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:supplier:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crm:supplier:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- CRM 供应商详情抽屉 -->
    <supplier-view-drawer ref="supplierViewRef" />
    <!-- 添加或修改CRM 供应商对话框 -->
    <el-dialog :title="title" v-model="open" width="1200px" append-to-body>
      <el-form ref="supplierRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="供应商名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入供应商名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="手机" prop="mobile">
              <el-input v-model="form.mobile" placeholder="请输入手机" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="负责人" prop="ownerUser">
              <el-input v-model="form.ownerUser" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所属地区" prop="detailAddress">
              <el-input v-model="form.detailAddress" placeholder="请输入供应商所属地区" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status">
                <el-option
                    v-for="item in crm_status"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="产品明细">
              <div class="project-block">
                <div class="project-toolbar">
                  <span class="project-toolbar__count">
                    已添加 <b>{{ projectRows.length }}</b> 个模型
                  </span>
                  <div class="project-toolbar__actions">
                    <el-button
                        type="primary"
                        plain
                        icon="Plus"
                        size="small"
                        @click="handleAddProjectRow()"
                    >新增产品</el-button>
                    <el-button
                        type="danger"
                        plain
                        icon="Delete"
                        size="small"
                        :disabled="!projectRows.length"
                        @click="handleClearProjectRows"
                    >清空明细</el-button>
                  </div>
                </div>
                <el-table
                    v-if="projectRows.length"
                    :data="projectRows"
                    border
                    size="small"
                    max-height="360"
                    class="project-table"
                    style="width: 100%"
                >
                  <el-table-column type="index" label="#" width="46" align="center" fixed="left" />
                  <el-table-column label="模型型号" width="170" fixed="left">
                    <template #default="scope">
                      <el-select
                          v-model="scope.row.projectId"
                          placeholder="请选择模型"
                          filterable
                          clearable
                          :loading="modelLoading"
                          style="width: 100%"
                      >
                        <el-option
                            v-for="item in modelSelectOptions"
                            :key="item.modelId"
                            :label="item.modelName"
                            :value="item.modelId"
                        />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="最小成本折扣" width="100" align="center">
                    <template #default="scope">
                      <el-input-number
                          v-model="scope.row.minCostDiscount"
                          :min="0"
                          :max="100"
                          :precision="2"
                          :controls="false"
                          placeholder="-"
                          style="width: 100%"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="最大成本折扣" width="100" align="center">
                    <template #default="scope">
                      <el-input-number
                          v-model="scope.row.maxCostDiscount"
                          :min="0"
                          :max="100"
                          :precision="2"
                          :controls="false"
                          placeholder="-"
                          style="width: 100%"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="最小售价折扣" width="100" align="center">
                    <template #default="scope">
                      <el-input-number
                          v-model="scope.row.minSellDiscount"
                          :min="0"
                          :max="100"
                          :precision="2"
                          :controls="false"
                          placeholder="-"
                          style="width: 100%"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="最大售价折扣" width="100" align="center">
                    <template #default="scope">
                      <el-input-number
                          v-model="scope.row.maxSellDiscount"
                          :min="0"
                          :max="100"
                          :precision="2"
                          :controls="false"
                          placeholder="-"
                          style="width: 100%"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="最小代理折扣" width="100" align="center">
                    <template #default="scope">
                      <el-input-number
                          v-model="scope.row.minAgentDiscount"
                          :min="0"
                          :max="100"
                          :precision="2"
                          :controls="false"
                          placeholder="-"
                          style="width: 100%"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="最大代理折扣" width="100" align="center">
                    <template #default="scope">
                      <el-input-number
                          v-model="scope.row.maxAgentDiscount"
                          :min="0"
                          :max="100"
                          :precision="2"
                          :controls="false"
                          placeholder="-"
                          style="width: 100%"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="预计日消耗金额" width="120" align="center">
                    <template #default="scope">
                      <el-input-number
                          v-model="scope.row.dailyUsageAmount"
                          :min="0"
                          :precision="2"
                          :controls="false"
                          placeholder="-"
                          style="width: 100%"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="90" align="center" fixed="right">
                    <template #default="scope">
                      <el-button
                          link
                          type="primary"
                          icon="Plus"
                          title="在下方插入一行"
                          @click="handleAddProjectRow(scope.$index)"
                      ></el-button>
                      <el-button
                          link
                          type="danger"
                          icon="Delete"
                          title="删除该行"
                          @click="handleRemoveProjectRow(scope.$index)"
                      ></el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty
                    v-else
                    description="暂无产品明细，点击「新增产品」开始添加"
                    :image-size="56"
                />
                <div v-if="duplicateModelNames.length" class="project-warning">
                  存在重复选择的模型：{{ duplicateModelNames.join("、") }}，请调整后再提交
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Supplier">
import { listSupplier, getSupplier, delSupplier, addSupplier, updateSupplier } from "@/api/crm/supplier"
import { listAllProduct } from "@/api/crm/product"
import SupplierViewDrawer from "./view"
const { proxy } = getCurrentInstance()

const { crm_status,crm_deleted } = useDict('crm_deleted','crm_status')

const supplierList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    mobile: undefined,
    email: undefined,
  },
  rules: {
    name: [
      { required: true, message: "供应商名称不能为空", trigger: "blur" }
    ],
    ownerUser: [
      { required: true, message: "负责人不能为空", trigger: "blur" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 产品明细行：统一取数组，避免模板里反复判空 */
const projectRows = computed(() => (Array.isArray(form.value.projects) ? form.value.projects : []))

/**
 * 详情接口是否返回了 projects 字段
 * <p>仅在返回了该字段（哪怕是空数组）时才提交关联列表，
 * 否则提交时不携带 projects，后端按「不变更关联」处理，避免误清空</p>
 */
const projectsProvided = ref(true)

/** 模型下拉选项：来源 /crm/product/listAll，字段映射 id→modelId、no→modelName（模型型号） */
const modelOptions = ref([])
const modelLoading = ref(false)
const modelLoaded = ref(false)

/** 加载模型下拉选项（只请求一次，后续复用缓存） */
function loadModelOptions() {
  if (modelLoaded.value || modelLoading.value) {
    return
  }
  modelLoading.value = true
  listAllProduct().then(response => {
    const list = Array.isArray(response.data) ? response.data : (response.rows || [])
    modelOptions.value = list.map(item => ({
      modelId: item.id,
      modelName: item.no || item.name || `编号 ${item.id}`
    }))
    modelLoaded.value = true
  }).catch(() => {}).finally(() => {
    modelLoading.value = false
  })
}

/** 模型下拉数据：补齐历史关联中引用的模型，避免只显示编号 */
const modelSelectOptions = computed(() => {
  const list = modelOptions.value
  const extra = []
  projectRows.value.forEach(row => {
    const current = row.projectId
    if (current == null || current === "") {
      return
    }
    const exist = list.some(item => String(item.modelId) === String(current))
    const added = extra.some(item => String(item.modelId) === String(current))
    if (!exist && !added) {
      extra.push({ modelId: current, modelName: `编号 ${current}（不在可选模型中）` })
    }
  })
  return extra.length ? [...extra, ...list] : list
})

/** 按 projectId 取模型展示名，未匹配时回落为编号 */
function modelNameOf(projectId) {
  const hit = modelSelectOptions.value.find(item => String(item.modelId) === String(projectId))
  return hit ? hit.modelName : `编号 ${projectId}`
}

/** 创建一行空白产品明细 */
function createProjectRow() {
  return {
    projectId: null,
    minCostDiscount: null,
    maxCostDiscount: null,
    minSellDiscount: null,
    maxSellDiscount: null,
    minAgentDiscount: null,
    maxAgentDiscount: null,
    dailyUsageAmount: null
  }
}

/** 详情返回的 projects → 表格行：字段与后端一致，缺键补空；无数据时返回空数组走空状态 */
function normalizeProjects(list) {
  if (!Array.isArray(list) || !list.length) {
    return []
  }
  return list.map(record => ({ ...createProjectRow(), ...record }))
}

/** 是否已填值（null / undefined / 空串均视为未填） */
function isFilled(val) {
  return val !== null && val !== undefined && val !== ""
}

/** 产品明细：插入一行。传下标时插到该行下方，不传时追加到末尾 */
function handleAddProjectRow(index) {
  if (!Array.isArray(form.value.projects)) {
    form.value.projects = []
  }
  const row = createProjectRow()
  if (typeof index === "number") {
    form.value.projects.splice(index + 1, 0, row)
  } else {
    form.value.projects.push(row)
  }
}

/** 产品明细：删除指定行（允许删空，删空后展示空状态） */
function handleRemoveProjectRow(index) {
  if (!Array.isArray(form.value.projects) || index < 0) {
    return
  }
  form.value.projects.splice(index, 1)
}

/** 产品明细：清空全部行 */
function handleClearProjectRows() {
  if (!projectRows.value.length) {
    return
  }
  proxy.$modal.confirm("是否清空全部产品明细？").then(() => {
    form.value.projects = []
  }).catch(() => {})
}

/** 重复选择的模型名称：同一 projectId 出现多次时列出，用于提示与提交拦截 */
const duplicateModelNames = computed(() => {
  const seen = new Set()
  const duplicated = []
  projectRows.value.forEach(row => {
    if (!isFilled(row.projectId)) {
      return
    }
    const key = String(row.projectId)
    if (seen.has(key)) {
      const name = modelNameOf(row.projectId)
      if (!duplicated.includes(name)) {
        duplicated.push(name)
      }
    } else {
      seen.add(key)
    }
  })
  return duplicated
})

/**
 * 产品明细提交前校验
 * <p>1. 每一行都必须选择模型；2. 同一模型不可重复；3. 各折扣的最小区间不得大于最大区间</p>
 *
 * @return true 校验通过
 */
function validateProjectRows() {
  const rows = projectRows.value
  const emptyIndex = rows.findIndex(row => !isFilled(row.projectId))
  if (emptyIndex >= 0) {
    proxy.$modal.msgError(`第 ${emptyIndex + 1} 行未选择模型，请选择模型或删除该行`)
    return false
  }
  if (duplicateModelNames.value.length) {
    proxy.$modal.msgError(`存在重复选择的模型：${duplicateModelNames.value.join("、")}，请调整后再提交`)
    return false
  }
  const ranges = [
    ["minCostDiscount", "maxCostDiscount", "成本折扣"],
    ["minSellDiscount", "maxSellDiscount", "售价折扣"],
    ["minAgentDiscount", "maxAgentDiscount", "代理折扣"]
  ]
  for (let i = 0; i < rows.length; i++) {
    const row = rows[i]
    for (const [minKey, maxKey, label] of ranges) {
      if (isFilled(row[minKey]) && isFilled(row[maxKey]) && Number(row[minKey]) > Number(row[maxKey])) {
        proxy.$modal.msgError(`第 ${i + 1} 行「${modelNameOf(row.projectId)}」的最小${label}不能大于最大${label}`)
        return false
      }
    }
  }
  return true
}

/**
 * 组装提交数据：仅保留有效行（已选模型），逐字段透传
 * <p>无有效行时 projects 为空数组，后端按「清空关联」处理</p>
 */
function buildSubmitPayload() {
  const payload = { ...form.value }
  payload.projects = projectRows.value
    .filter(row => isFilled(row.projectId))
    .map(row => ({
      projectId: row.projectId,
      minCostDiscount: row.minCostDiscount,
      maxCostDiscount: row.maxCostDiscount,
      minSellDiscount: row.minSellDiscount,
      maxSellDiscount: row.maxSellDiscount,
      minAgentDiscount: row.minAgentDiscount,
      maxAgentDiscount: row.maxAgentDiscount,
      dailyUsageAmount: row.dailyUsageAmount
    }))
  // 详情未返回 projects 且本次也未新增行时不提交该字段，保持后端关联不变
  if (!projectsProvided.value && !payload.projects.length) {
    delete payload.projects
  }
  return payload
}

/** 查询CRM 供应商列表 */
function getList() {
  loading.value = true
  listSupplier(queryParams.value).then(response => {
    supplierList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  projectsProvided.value = true
  form.value = {
    id: null,
    name: null,
    ownerUser: null,
    mobile: null,
    email: null,
    detailAddress: null,
    webPwd: null,
    remark: null,
    projects: [createProjectRow()],
  }
  proxy.resetForm("supplierRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  loadModelOptions()
  open.value = true
  title.value = "添加CRM 供应商"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  loadModelOptions()
  const _id = row.id || ids.value
  getSupplier(_id).then(response => {
    const detail = response.data || {}
    projectsProvided.value = Array.isArray(detail.projects)
    form.value = { ...detail, projects: normalizeProjects(detail.projects) }
    open.value = true
    title.value = "修改CRM 供应商"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["supplierRef"].validate(valid => {
    if (valid) {
      if (!validateProjectRows()) {
        return
      }
      const payload = buildSubmitPayload()
      if (payload.id != null) {
        updateSupplier(payload).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addSupplier(payload).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除CRM 供应商:"' + row.name + '"的数据项？').then(function() {
    return delSupplier(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 详情按钮操作 */
function handleViewData(row) {
  proxy.$refs["supplierViewRef"].open(row.id)
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('crm/supplier/export', {
    ...queryParams.value
  }, `supplier_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
.project-block {
  width: 100%;
}

.project-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 8px;
}

.project-toolbar__count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.project-toolbar__count b {
  color: var(--el-color-primary);
  font-weight: 600;
  padding: 0 2px;
}

.project-table :deep(.el-table__cell) {
  padding: 4px 0;
}

.project-table :deep(.el-input-number .el-input__inner) {
  text-align: center;
}

.project-warning {
  margin-top: 6px;
  font-size: 12px;
  line-height: 18px;
  color: var(--el-color-danger);
}
</style>
