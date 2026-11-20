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
      <el-form-item label="渠道类型" prop="supplierType">
        <el-select
          v-model="queryParams.supplierType"
          placeholder="全部"
          clearable
          style="width: 140px"
        >
          <el-option
            v-for="item in supplierTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
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
      <el-table-column label="渠道类型" align="center" prop="supplierType" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.supplierType === 2 ? 'warning' : 'success'" size="small" effect="plain">
            {{ supplierTypeLabel(scope.row.supplierType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="负责人" align="center" prop="ownerUser" fixed="left"/>
      <el-table-column label="状态" align="center" prop="status" width="100" fixed="left">
        <template #default="scope">
          <dict-tag :options="crm_status" :value="scope.row.status" />
        </template>
      </el-table-column>
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
            <el-form-item label="渠道类型" prop="supplierType">
              <el-radio-group v-model="form.supplierType">
                <el-radio
                    v-for="item in supplierTypeOptions"
                    :key="item.value"
                    :value="item.value"
                >{{ item.label }}</el-radio>
              </el-radio-group>
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
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                <el-option
                    v-for="item in statusOptions"
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
                  <el-table-column label="模型型号" width="220" fixed="left">
                    <template #default="scope">
                      <!-- 模型取自 crm_model_product；选中后按 project_id 懒加载所属渠道并带出折扣 -->
                      <el-select
                          v-model="scope.row.projectId"
                          placeholder="请选择模型"
                          filterable
                          clearable
                          :loading="modelOptionsLoading"
                          style="width: 100%"
                          @change="handleModelChange(scope.row)"
                      >
                        <el-option
                            v-for="item in modelOptionsOf(scope.row)"
                            :key="item.modelId"
                            :label="item.modelLabel"
                            :value="item.modelId"
                        />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="模型所属供应商" width="190" fixed="left">
                    <template #default="scope">
                      <!-- 级联字段：选项为该模型在 crm_supplier_project 中的全部可报价渠道；留空表示取自当前供应商 -->
                      <el-select
                          v-model="scope.row.projectSupplierId"
                          placeholder="默认当前供应商"
                          filterable
                          clearable
                          :loading="isChannelLoading(scope.row)"
                          style="width: 100%"
                          @visible-change="visible => visible && loadRowChannels(scope.row)"
                          @change="handleProjectSupplierChange(scope.row)"
                      >
                        <el-option
                            v-for="item in supplierOptionsOf(scope.row)"
                            :key="item.id"
                            :label="item.name"
                            :value="item.id"
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
                <div v-if="duplicateProjects.length" class="project-warning">
                  存在重复的「模型所属供应商 + 模型型号」：{{ duplicateProjects.join("、") }}，同一供应商的同一模型只能维护一条
                </div>
                <div class="project-tip">
                  选择模型型号后，「模型所属供应商」下拉会列出该模型在渠道报价中的全部可报价渠道；选中渠道后带出该渠道的折扣，留空表示取自当前供应商
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
import { listSupplier, getSupplier, delSupplier, addSupplier, updateSupplier, getProjectChannels } from "@/api/crm/supplier"
import { listAllProduct } from "@/api/crm/product"
import SupplierViewDrawer from "./view"
const { proxy } = getCurrentInstance()

const { crm_deleted,crm_status } = useDict('crm_deleted','crm_status')

/**
 * 状态下拉选项：字典值为字符串（'0'/'1'/'2'），而 form.status 是后端返回的数值，
 * el-select 按值严格匹配，字符串选项会导致已选状态回显不出来，这里统一转成数值。
 */
const statusOptions = computed(() =>
  (crm_status.value || []).map(item => ({ ...item, value: Number(item.value) }))
)

/**
 * 渠道类型：与后端 crm_supplier.supplier_type 约定一致（1 自营、2 三方）
 * <p>枚举值固定且仅两档，直接用本地常量，避免字典缺失导致下拉为空、列表回显成数字</p>
 */
const supplierTypeOptions = [
  { label: "自营", value: 1 },
  { label: "三方", value: 2 }
]

/** 渠道类型文案：空值（历史脏数据）按自营展示，与数据库默认值口径一致 */
function supplierTypeLabel(value) {
  const hit = supplierTypeOptions.find(item => item.value === value)
  return hit ? hit.label : "自营"
}

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
    supplierType: undefined,
    mobile: undefined,
    email: undefined,
  },
  rules: {
    name: [
      { required: true, message: "供应商名称不能为空", trigger: "blur" }
    ],
    supplierType: [
      { required: true, message: "请选择渠道类型", trigger: "change" }
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

/**
 * 全量模型下拉数据：来源 /crm/product/listAll（仅启用、未删除）
 * <p>模型型号与「模型所属供应商」互不级联——同一行可以先选型号再选供应商，
 * 也可以只选型号（供应商留空表示取自当前供应商）</p>
 */
const modelOptions = ref([])
const modelOptionsLoading = ref(false)
const modelOptionsLoaded = ref(false)

/** 加载全量模型下拉选项（只请求一次，后续复用缓存） */
function loadModelOptions() {
  if (modelOptionsLoaded.value || modelOptionsLoading.value) {
    return
  }
  modelOptionsLoading.value = true
  listAllProduct().then(res => {
    const list = Array.isArray(res.data) ? res.data : []
    modelOptions.value = list.map(item => ({
      modelId: item.id,
      modelNo: item.no,
      modelName: item.name,
      modelLabel: item.no && item.name
        ? `${item.no}（${item.name}）`
        : (item.no || item.name || `编号 ${item.id}`)
    }))
    modelOptionsLoaded.value = true
  }).catch(() => {}).finally(() => {
    modelOptionsLoading.value = false
  })
}

/**
 * 某行的模型下拉：全量模型列表，不按模型所属供应商过滤
 * <p>同时补齐该行已引用的模型，避免历史数据或模型下架后显示为空白</p>
 */
function modelOptionsOf(row) {
  const list = [...modelOptions.value]
  const current = row.projectId
  if (isFilled(current) && !list.some(item => String(item.modelId) === String(current))) {
    list.unshift({ modelId: current, modelLabel: `编号 ${current}（模型已不存在或已停用）` })
  }
  return list
}

/** 按行取模型展示名，未匹配时回落为编号 */
function modelNameOf(row) {
  const hit = modelOptionsOf(row).find(item => String(item.modelId) === String(row.projectId))
  return hit ? hit.modelLabel : `编号 ${row.projectId}`
}

/**
 * 「模型 → 报价渠道」懒加载缓存：key 为模型ID
 * <p>只有在选中模型时才请求 GET /crm/supplier/project-channels，
 * 数据源是 crm_supplier_project 按 project_id 反查 supplier_id（关联 crm_supplier 取渠道名称）</p>
 */
const projectChannelCache = reactive({})

/** 模型报价渠道加载中标记，key 为模型ID */
const projectChannelLoading = reactive({})

/** 按模型ID懒加载报价渠道（同一模型的多行复用同一次请求结果） */
function loadProjectChannels(projectId) {
  const key = String(projectId)
  if (projectChannelCache[key] || projectChannelLoading[key]) {
    return Promise.resolve(projectChannelCache[key] || [])
  }
  projectChannelLoading[key] = true
  return getProjectChannels(projectId).then(res => {
    const list = Array.isArray(res.data) ? res.data : []
    projectChannelCache[key] = list
    // 顺手记住渠道名称，避免下拉回显为编号
    list.forEach(item => rememberChannel(item.supplierId, item.supplierName))
    return list
  }).catch(() => {
    // 接口异常时按「没有报价渠道」处理，不阻塞手工填写
    projectChannelCache[key] = []
    return []
  }).finally(() => {
    projectChannelLoading[key] = false
  })
}

/** 某行模型的可报价渠道是否加载中 */
function isChannelLoading(row) {
  return isFilled(row.projectId) && Boolean(projectChannelLoading[String(row.projectId)])
}

/** 展开「模型所属供应商」下拉时按需加载该行模型的可报价渠道 */
function loadRowChannels(row) {
  if (isFilled(row.projectId)) {
    loadProjectChannels(row.projectId)
  }
}

/** 清空懒加载缓存（打开新增/修改弹窗时重置，避免拿到过期的渠道与折扣） */
function clearProjectChannelCache() {
  Object.keys(projectChannelCache).forEach(key => delete projectChannelCache[key])
}

/**
 * 渠道名称补充池
 * <p>/crm/supplier/options 只返回启用渠道，而级联带出的报价渠道可能是暂停/未启用的，
 * 把接口返回的 id + 名称缓存下来，避免下拉回显成编号</p>
 */
const channelExtras = ref([])

/** 记住一个渠道的 id 与名称，供「模型所属供应商」下拉回显 */
function rememberChannel(supplierId, supplierName) {
  if (!isFilled(supplierId) || !isFilled(supplierName)) {
    return
  }
  if (channelExtras.value.some(item => String(item.id) === String(supplierId))) {
    return
  }
  channelExtras.value.push({ id: supplierId, name: supplierName })
}

/** ID 相等判断（兼容数字与字符串） */
function sameId(a, b) {
  return isFilled(a) && isFilled(b) && String(a) === String(b)
}

/** 把渠道记录的四个折扣写入行；无记录时清空，避免沿用上一个模型带出的值 */
function applyDiscount(row, record) {
  row.minCostDiscount = record ? record.minCostDiscount : null
  row.maxCostDiscount = record ? record.maxCostDiscount : null
  row.minSellDiscount = record ? record.minSellDiscount : null
  row.maxSellDiscount = record ? record.maxSellDiscount : null
}

/**
 * 选中模型型号：清空上一模型的渠道与折扣，并懒加载该模型的可报价渠道
 * <p>换模型后原来的「模型所属供应商」与折扣都已失效，统一回落为「留空 = 当前供应商」；
 * 可报价渠道（crm_supplier_project 按 project_id 反查 supplier_id）只用于撑满下拉选项，
 * 具体选哪个渠道由用户在「模型所属供应商」下拉中选择，选中后再带出该渠道的折扣。</p>
 */
function handleModelChange(row) {
  row.projectSupplierId = null
  applyDiscount(row, null)
  if (!isFilled(row.projectId)) {
    return
  }
  loadProjectChannels(row.projectId)
}

/**
 * 某个渠道对某个模型的报价记录（含四段折扣）
 * <p>渠道为空时由调用方按当前供应商传入，保证「留空 = 当前供应商」也能带出折扣</p>
 *
 * @return 命中的记录，没有则 null
 */
function findChannelRecord(projectId, channelId) {
  if (!isFilled(projectId) || !isFilled(channelId)) {
    return null
  }
  const list = projectChannelCache[String(projectId)] || []
  return list.find(item => sameId(item.supplierId, channelId)) || null
}

/**
 * 手动切换「模型所属供应商」：带出该渠道对该模型的折扣
 * <p>该渠道没有该模型的报价记录时清空折扣，避免沿用上一个渠道的数值</p>
 */
function handleProjectSupplierChange(row) {
  const channelId = isFilled(row.projectSupplierId) ? row.projectSupplierId : form.value.id
  const record = findChannelRecord(row.projectId, channelId)
  if (record) {
    rememberChannel(record.supplierId, record.supplierName)
  }
  applyDiscount(row, record)
}

/**
 * 某行「模型所属供应商」下拉选项
 * <p>= 该模型在 crm_supplier_project 中的全部可报价渠道 + 当前供应商 + 行内已选渠道兜底。
 * 渠道全部来自懒加载结果，因此暂停/未启用的渠道也能正常显示名称</p>
 */
function supplierOptionsOf(row) {
  const list = []
  const push = (id, name) => {
    if (!isFilled(id) || list.some(item => sameId(item.id, id))) {
      return
    }
    list.push({ id, name })
  }
  const cached = isFilled(row.projectId) ? (projectChannelCache[String(row.projectId)] || []) : []
  cached.forEach(item => push(item.supplierId, item.supplierName))
  push(form.value.id, form.value.name ? `${form.value.name}（当前供应商）` : `编号 ${form.value.id}（当前供应商）`)
  if (isFilled(row.projectSupplierId)) {
    push(row.projectSupplierId, channelNameOf(row.projectSupplierId, row.projectSupplierName))
  }
  return list
}

/** 按渠道ID取名称：先查名称池，再取调用方给的兜底名称，最后回落为编号 */
function channelNameOf(supplierId, fallbackName) {
  const hit = channelExtras.value.find(item => sameId(item.id, supplierId))
  if (hit) {
    return hit.name
  }
  if (isFilled(fallbackName)) {
    return fallbackName
  }
  return `编号 ${supplierId}`
}

/** 按渠道ID取名称，供重复提示文案使用；为空表示该行取自当前供应商 */
function supplierNameOf(supplierId) {
  if (!isFilled(supplierId)) {
    return "当前供应商"
  }
  return channelNameOf(supplierId)
}

/** 行有效模型所属供应商：未选择时视为当前供应商（后端保存时按当前供应商落库） */
function rowSupplierId(row) {
  return isFilled(row.projectSupplierId) ? row.projectSupplierId : form.value.id
}

/** 创建一行空白产品明细 */
function createProjectRow() {
  return {
    projectSupplierId: null,
    projectId: null,
    minCostDiscount: null,
    maxCostDiscount: null,
    minSellDiscount: null,
    maxSellDiscount: null
  }
}

/** 详情返回的 projects → 表格行：字段与后端一致，缺键补空；无数据时返回空数组走空状态 */
function normalizeProjects(list) {
  if (!Array.isArray(list) || !list.length) {
    return []
  }
  return list.map(record => {
    // 详情已带出模型所属供应商名称，记入补充池避免下拉回显为编号
    rememberChannel(record.projectSupplierId, record.projectSupplierName)
    return { ...createProjectRow(), ...record }
  })
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
  // 模型所属供应商可留空（表示取自当前供应商），新增行不再预填，由用户按需选择
  loadModelOptions()
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

/**
 * 重复的「模型所属供应商 + 模型型号」
 * <p>唯一性判定维度是「模型所属供应商 + 模型型号」，与下拉是否级联无关：
 * 未选模型所属供应商的行按当前供应商计算（为空时同样参与判重），
 * 因此不同供应商下的同一型号不算重复——正是本表要支持的多来源报价场景。</p>
 */
const duplicateProjects = computed(() => {
  const seen = new Set()
  const duplicated = []
  projectRows.value.forEach(row => {
    if (!isFilled(row.projectId)) {
      return
    }
    const supplierId = rowSupplierId(row)
    const supplierKey = isFilled(supplierId) ? String(supplierId) : ""
    const key = `${supplierKey}:${row.projectId}`
    if (seen.has(key)) {
      const text = `${supplierNameOf(supplierId)} / ${modelNameOf(row)}`
      if (!duplicated.includes(text)) {
        duplicated.push(text)
      }
    } else {
      seen.add(key)
    }
  })
  return duplicated
})

/**
 * 产品明细提交前校验
 * <p>1. 每一行都必须选择模型（模型所属供应商可为空，为空表示取自当前供应商）；
 * 2. 同一「模型所属供应商 + 模型」不可重复；
 * 3. 各折扣的最小区间不得大于最大区间</p>
 *
 * @return true 校验通过
 */
function validateProjectRows() {
  // 完全空白的行（既没选供应商也没选模型）视为用户未使用的占位行，忽略不校验
  const rows = projectRows.value.filter(row => isFilled(row.projectId) || isFilled(row.projectSupplierId))
  const emptyIndex = rows.findIndex(row => !isFilled(row.projectId))
  if (emptyIndex >= 0) {
    proxy.$modal.msgError(`第 ${emptyIndex + 1} 行未选择模型，请选择模型或删除该行`)
    return false
  }
  if (duplicateProjects.value.length) {
    proxy.$modal.msgError(`存在重复的「模型所属供应商 + 模型」：${duplicateProjects.value.join("、")}，同一供应商的同一模型只能维护一条`)
    return false
  }
  const ranges = [
    ["minCostDiscount", "maxCostDiscount", "成本折扣"],
    ["minSellDiscount", "maxSellDiscount", "售价折扣"]
  ]
  for (let i = 0; i < rows.length; i++) {
    const row = rows[i]
    for (const [minKey, maxKey, label] of ranges) {
      if (isFilled(row[minKey]) && isFilled(row[maxKey]) && Number(row[minKey]) > Number(row[maxKey])) {
        proxy.$modal.msgError(`第 ${i + 1} 行「${modelNameOf(row)}」的最小${label}不能大于最大${label}`)
        return false
      }
    }
  }
  return true
}

/**
 * 组装提交数据：仅保留有效行（已选模型），逐字段透传
 * <p>无有效行时 projects 为空数组，后端按「清空关联」处理；
 * supplierId 不再由前端提交，统一由后端按当前供应商写入，
 * projectSupplierId 为空表示该模型取自当前供应商自身</p>
 */
function buildSubmitPayload() {
  const payload = { ...form.value }
  payload.projects = projectRows.value
    .filter(row => isFilled(row.projectId))
    .map(row => ({
      projectSupplierId: isFilled(row.projectSupplierId) ? row.projectSupplierId : null,
      projectId: row.projectId,
      minCostDiscount: row.minCostDiscount,
      maxCostDiscount: row.maxCostDiscount,
      minSellDiscount: row.minSellDiscount,
      maxSellDiscount: row.maxSellDiscount
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
  // 每次开弹窗重置懒加载缓存与渠道名称补充池，避免沿用上一次的渠道/折扣
  clearProjectChannelCache()
  channelExtras.value = []
  form.value = {
    id: null,
    name: null,
    // 渠道类型默认自营，与数据库列默认值一致
    supplierType: 1,
    ownerUser: null,
    mobile: null,
    email: null,
    detailAddress: null,
    webPwd: null,
    status: 1,
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
    form.value = {
      ...detail,
      // 历史数据 supplier_type 为空时按自营回显，避免单选组空白导致无法提交
      supplierType: isFilled(detail.supplierType) ? Number(detail.supplierType) : 1,
      projects: normalizeProjects(detail.projects)
    }
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

.project-tip {
  margin-top: 6px;
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}
</style>
