<template>
  <el-drawer title="CRM 客户详情" v-model="visible" direction="rtl" size="60%" append-to-body :before-close="handleClose" class="detail-drawer">
    <div v-loading="loading" class="drawer-content">
      <h4 class="section-header">基本信息</h4>
      <el-row :gutter="20" class="mb8">
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">客户名称：</label>
            <span class="info-value plaintext">
              {{ info.name }}
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="info-item">
            <label class="info-label">客户状态：</label>
            <span class="info-value plaintext">
              {{ info.status }}
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
            <label class="info-label">所属行业：</label>
            <span class="info-value plaintext">
              {{ info.industry }}
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
      </el-row>
      <el-row :gutter="20" class="mb8">
        <el-col :span="24">
          <div class="info-item">
            <label class="info-label">关联渠道：</label>
            <span class="info-value plaintext">
              <template v-if="channelList.length">
                <el-tag v-for="item in channelList" :key="item.supplierId" size="small" class="channel-tag">
                  {{ item.supplierName || item.supplierCode || item.supplierId }}
                </el-tag>
              </template>
              <span v-else class="text-placeholder">未关联渠道</span>
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
      </el-row>
    </div>
  </el-drawer>
</template>

<script setup name="CustomerViewDrawer">
import { getCustomer, getCustomerSuppliers } from '@/api/crm/customer'


const visible = ref(false)
const loading = ref(false)
const info = reactive({})
/** 客户已关联的渠道 */
const channelList = ref([])

const open = async (id) => {
  visible.value = true
  loading.value = true
  try {
    const res = await getCustomer(id)
    Object.assign(info, res.data || {})
    const supRes = await getCustomerSuppliers(id)
    channelList.value = supRes.data || []
  } catch (error) {
    console.error('获取CRM 客户信息失败:', error)
  } finally {
    loading.value = false
  }
}

function handleClose() {
  visible.value = false
  channelList.value = []
  Object.keys(info).forEach(key => delete info[key])
}

defineExpose({ open })
</script>

<style scoped>
.channel-tag {
  margin: 0 6px 6px 0;
}
.text-placeholder {
  color: #c0c4cc;
}
</style>
