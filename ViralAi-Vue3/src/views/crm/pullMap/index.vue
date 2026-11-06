<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="渠道" prop="supplierId">
        <el-select v-model="queryParams.supplierId" placeholder="全部渠道" filterable clearable style="width: 180px">
          <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="映射类型" prop="mapType">
        <el-select v-model="queryParams.mapType" placeholder="全部类型" clearable style="width: 150px">
          <el-option label="客户映射" value="CUSTOMER" />
          <el-option label="模型映射" value="PRODUCT" />
        </el-select>
      </el-form-item>
      <el-form-item label="上游标识" prop="upstreamKey">
        <el-input
          v-model="queryParams.upstreamKey"
          placeholder="上游用户名 / 模型标识"
          clearable
          style="width: 200px"
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['crm:pullMap:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="!ids.length" @click="handleDelete" v-hasPermi="['crm:pullMap:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="拉取回来的流水用的是上游标识，必须映射到本系统主键才能对账：客户映射不到 → 流水不入库；模型映射不到 → 退化为上游标识，对账会判为「模型不在合同内」。"
    />

    <el-table v-loading="loading" :data="mapList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="渠道" align="center" prop="supplierName" min-width="140" show-overflow-tooltip />
      <el-table-column label="渠道账户" align="center" min-width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.accountName || "渠道通用" }}</template>
      </el-table-column>
      <el-table-column label="映射类型" align="center" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.mapType === 'CUSTOMER' ? 'primary' : 'success'" effect="plain">
            {{ scope.row.mapType === "CUSTOMER" ? "客户" : "模型" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上游标识" align="center" prop="upstreamKey" min-width="170" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.upstreamKey || "（账户默认客户）" }}</template>
      </el-table-column>
      <el-table-column label="上游变体" align="center" prop="upstreamVariant" min-width="160" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.upstreamVariant || "-" }}</template>
      </el-table-column>
      <el-table-column label="映射目标" align="center" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.mapType === 'CUSTOMER'">{{ scope.row.customerName || `客户 ${scope.row.targetId}` }}</span>
          <span v-else>{{ scope.row.targetCode }}{{ scope.row.modelNo ? `（${scope.row.modelNo}）` : "" }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
            {{ scope.row.status === 1 ? "有效" : "失效" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['crm:pullMap:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['crm:pullMap:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="640px" append-to-body>
      <el-form ref="mapRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="渠道" prop="supplierId">
          <el-select v-model="form.supplierId" placeholder="请选择渠道" filterable style="width: 100%" @change="handleFormSupplierChange">
            <el-option v-for="item in supplierOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道账户" prop="supplierAccountId">
          <el-select v-model="form.supplierAccountId" placeholder="不选表示该渠道通用" filterable clearable style="width: 100%">
            <el-option label="该渠道通用" :value="0" />
            <el-option
              v-for="item in formAccountOptions"
              :key="item.id"
              :label="`${item.accountName}${item.pullChannel ? '（' + item.pullChannel + '）' : ''}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="映射类型" prop="mapType">
          <el-radio-group v-model="form.mapType">
            <el-radio value="CUSTOMER">客户</el-radio>
            <el-radio value="PRODUCT">模型</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="上游标识" prop="upstreamKey">
          <el-input
            v-model="form.upstreamKey"
            :placeholder="form.mapType === 'CUSTOMER' ? '上游用户名，留空表示该账户的默认客户' : '上游模型标识，如 gpt-4o / v6'"
            maxlength="128"
          />
        </el-form-item>
        <el-form-item v-if="form.mapType === 'PRODUCT'" label="上游变体" prop="upstreamVariant">
          <el-input v-model="form.upstreamVariant" placeholder="如 720p|text_to_video，无变体留空" maxlength="128" />
          <div class="form-tip">pixverse 的价格随清晰度与能力变化，变体用于区分不同价格的用量。</div>
        </el-form-item>
        <el-form-item v-if="form.mapType === 'CUSTOMER'" label="目标客户" prop="targetId">
          <el-select v-model="form.targetId" placeholder="请选择客户" filterable style="width: 100%">
            <el-option v-for="item in customerOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="目标模型" prop="targetCode">
          <el-select v-model="form.targetCode" placeholder="请选择模型编码" filterable style="width: 100%">
            <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="`${item.productCode || item.no}（${item.name || '-'}）`"
              :value="item.productCode || item.no"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">有效</el-radio>
            <el-radio :value="0">失效</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PullMap">
import { listPullMap, getPullMap, addPullMap, updatePullMap, delPullMap } from "@/api/crm/pullMap"
import { getSupplierOptions } from "@/api/crm/supplier"
import { listSupplierAccount } from "@/api/crm/supplierAccount"
import { getCustomerOptions } from "@/api/crm/customer"
import { listAllProduct } from "@/api/crm/product"

const { proxy } = getCurrentInstance()

const mapList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const total = ref(0)
const title = ref("")
const submitting = ref(false)
const supplierOptions = ref([])
const customerOptions = ref([])
const productOptions = ref([])
const formAccountOptions = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    supplierId: undefined,
    supplierAccountId: undefined,
    mapType: undefined,
    upstreamKey: undefined,
    status: undefined
  },
  rules: {
    supplierId: [{ required: true, message: "请选择渠道", trigger: "change" }],
    mapType: [{ required: true, message: "请选择映射类型", trigger: "change" }],
    targetId: [{ required: true, message: "请选择目标客户", trigger: "change" }],
    targetCode: [{ required: true, message: "请选择目标模型", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listPullMap(queryParams.value)
    .then(res => {
      mapList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
}

function reset() {
  form.value = {
    id: undefined,
    supplierId: undefined,
    supplierAccountId: 0,
    mapType: "CUSTOMER",
    upstreamKey: "",
    upstreamVariant: "",
    targetId: undefined,
    targetCode: undefined,
    status: 1,
    remark: undefined
  }
  formAccountOptions.value = []
  proxy.resetForm("mapRef")
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "新增流水映射"
}

function handleUpdate(row) {
  reset()
  getPullMap(row.id).then(res => {
    form.value = { ...res.data, upstreamKey: res.data.upstreamKey || "", upstreamVariant: res.data.upstreamVariant || "" }
    loadFormAccounts()
    open.value = true
    title.value = "修改流水映射"
  })
}

/** 表单内的账户选项：渠道变化时重新加载，避免选到别家渠道的账户 */
function loadFormAccounts() {
  if (!form.value.supplierId) {
    formAccountOptions.value = []
    return
  }
  listSupplierAccount({ pageNum: 1, pageSize: 500, supplierId: form.value.supplierId }).then(res => {
    formAccountOptions.value = res.rows || []
  })
}

function handleFormSupplierChange() {
  form.value.supplierAccountId = 0
  loadFormAccounts()
}

function submitForm() {
  mapRef.value.validate(valid => {
    if (!valid) {
      return
    }
    submitting.value = true
    const payload = { ...form.value }
    if (payload.mapType === "CUSTOMER") {
      payload.targetCode = undefined
      payload.upstreamVariant = ""
    } else {
      payload.targetId = undefined
    }
    const action = payload.id ? updatePullMap(payload) : addPullMap(payload)
    action.then(() => {
      proxy.$modal.msgSuccess(payload.id ? "修改成功" : "新增成功")
      open.value = false
      getList()
    }).finally(() => {
      submitting.value = false
    })
  })
}

function cancel() {
  open.value = false
  reset()
}

function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value
  if (!delIds.length) {
    return
  }
  proxy.$modal.confirm(`确认删除选中的 ${delIds.length} 条映射？删除后对应流水将无法解析客户，重新拉取时会被跳过。`).then(() => {
    return Promise.all(delIds.map(id => delPullMap(id)))
  }).then(() => {
    proxy.$modal.msgSuccess("删除成功")
    ids.value = []
    getList()
  }).catch(() => {})
}

const mapRef = ref()

getSupplierOptions().then(res => {
  supplierOptions.value = res.data || []
})
getCustomerOptions().then(res => {
  customerOptions.value = res.data || []
})
listAllProduct().then(res => {
  // 兼容后端返回 data 数组或 rows 两种结构
  productOptions.value = Array.isArray(res.data) ? res.data : (res.rows || [])
})
getList()
</script>

<style scoped>
.form-tip {
  width: 100%;
  line-height: 1.5;
  padding-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
