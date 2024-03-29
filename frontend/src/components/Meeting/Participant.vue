<template>
  <div class="p-2">
    <div 
      v-for="participant in participants" 
      :key="participant.pk" 
      class="d-flex flex-row justify-content-between align-items-center py-2 px-3 waiting-participant"
    >
      <span class="fw-bold">
        {{ participant.nickname }}
        <i v-if="parseInt(participant.id) === this.$store.state.meeting.hostId" class="host bi bi-star-fill ps-1"></i> 
      </span>
      <div>
        <el-button type="danger" plain round size="small"
          @click="clickKickParticipant(participant.id, participant.nickname)"
          v-if="this.$store.state.meeting.hostId === this.$store.state.user.id && this.$store.state.user.nickname !== participant.nickname"
        >강퇴</el-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, computed } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Mute } from '@element-plus/icons-vue'
import { useStore } from 'vuex'
import axios from 'axios'

export default defineComponent({
  name: 'Participant',
  setup() {
    const store = useStore()
    const participants = computed(() => store.state.participants)
    const kickParticipant = function (userId: number) {
      axios.delete(`/meeting/${store.state.meeting.id}/users/${userId}/forcedExit`,
        { headers: { Authorization: `Bearer ${localStorage.getItem("token")}` }}
      ).then(res => {
        ElMessage({
          type: 'success',
          message: '강퇴되었습니다.',
        })
      }).catch(err => {
      }) 
    }

    const clickKickParticipant = function (userId: number, userNickname: string) {
      ElMessageBox.confirm(
        `${userNickname}님을 강퇴하시겠습니까?`,
        '강퇴',
        {
          confirmButtonText: '강퇴',
          cancelButtonText: '취소',
          type: 'warning',
        }
      )
        .then(() => {
          kickParticipant(userId)
        })
        .catch(() => {
          ElMessage({
            type: 'info',
            message: '취소되었습니다.',
          })
        })
    }

    return { participants, Mute, clickKickParticipant }
  }
})
</script>

<style scoped>
.waiting-participant {
  box-shadow: inset 3px 3px 8px 0 rgba(0, 0, 0, 0.2),
              inset -6px -6px 10px 0 rgba(255, 255, 255, 0.5);
  border-radius: 10px;
  margin: 10px;
  padding: 18px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  line-height: 2;
}

.host {
  color: rgb(255, 120, 120);
}
</style>