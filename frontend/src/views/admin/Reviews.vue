<template>
  <div class="admin-reviews">
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <h3>评论管理</h3>
        <div>
          <el-input v-model="filters.userId" placeholder="用户ID" style="width:120px;margin-right:8px"></el-input>
          <el-input v-model="filters.vehicleId" placeholder="车辆ID" style="width:120px;margin-right:8px"></el-input>
          <el-select v-model="filters.rating" placeholder="评分" clearable style="width:100px;margin-right:8px">
            <el-option v-for="r in [5,4,3,2,1]" :key="r" :label="r" :value="r"></el-option>
          </el-select>
          <el-button type="primary" @click="fetch">筛选</el-button>
        </div>
      </div>

      <el-table :data="reviews" style="width:100%;margin-top:16px">
        <el-table-column prop="id" label="订单ID" width="100"/>
        <el-table-column prop="userName" label="用户" width="140"/>
        <el-table-column prop="vehicleName" label="车辆" width="140"/>
        <el-table-column prop="rating" label="评分" width="80"/>
        <el-table-column label="评论" min-width="300">
          <template #default="{ row }">
            <div class="review-text">{{ row.review }}</div>
            <div v-if="row.reviewImages" class="thumbs">
              <img v-for="(img, idx) in parseImages(row.reviewImages)" :key="idx" :src="img" @click="openLightbox(img)"/>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="reviewTime" label="时间" width="180"/>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="deleteReview(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display:flex;justify-content:space-between;align-items:center;margin-top:12px">
        <el-pagination :current-page="page" :page-size="pageSize" :total="total" @current-change="onPageChange" layout="prev, pager, next, jumper"></el-pagination>
        <div>每页：<el-select v-model="pageSize" @change="onPageSizeChange" size="small" style="width:100px">
          <el-option label="10" :value="10"></el-option>
          <el-option label="20" :value="20"></el-option>
          <el-option label="50" :value="50"></el-option>
        </el-select></div>
      </div>
    </el-card>

    <el-dialog :visible.sync="lightboxVisible" width="60%">
      <img :src="lightboxSrc" style="width:100%"/>
    </el-dialog>
  </div>
</template>

<script>
import api from '@/api'
export default {
  name: 'AdminReviews',
  data() {
    return {
      reviews: [],
      page: 1,
      pageSize: 20,
      total: 0,
      filters: {
        rating: null,
        vehicleId: '',
        userId: ''
      },
      lightboxVisible: false,
      lightboxSrc: ''
    }
  },
  mounted() {
    this.fetch()
  },
  methods: {
    async fetch() {
      const params = {
        page: this.page,
        pageSize: this.pageSize
      }
      if (this.filters.rating) params.rating = this.filters.rating
      if (this.filters.vehicleId) params.vehicleId = this.filters.vehicleId
      if (this.filters.userId) params.userId = this.filters.userId

      const res = await api.admin.reviews.list(params)
      if (res && res.code === 200) {
        const d = res.data
        this.reviews = d.data
        this.page = d.page
        this.pageSize = d.pageSize
        this.total = d.total
      }
    },
    parseImages(imagesStr) {
      try {
        return JSON.parse(imagesStr)
      } catch (e) {
        return imagesStr ? [imagesStr] : []
      }
    },
    openLightbox(src) {
      this.lightboxSrc = src
      this.lightboxVisible = true
    },
    async deleteReview(orderId) {
      this.$confirm('确认删除该评论吗？', '提示')
        .then(async () => {
          const res = await api.admin.reviews.delete(orderId)
           if (res && res.code === 200) {
             this.$message.success('删除成功')
             this.fetch()
           } else {
             this.$message.error(res ? res.message : '删除失败')
           }
         })
         .catch(() => {})
     },
    onPageChange(p) {
      this.page = p
      this.fetch()
    },
    onPageSizeChange() {
      this.page = 1
      this.fetch()
    }
  }
}
</script>

<style scoped>
.admin-reviews img { width: 80px; height: 60px; object-fit: cover; margin-right: 8px; cursor: pointer }
.thumbs { margin-top: 6px; display:flex }
.review-text { max-height: 48px; overflow: hidden; text-overflow: ellipsis }
</style>
