<template>
  <div class="d-flex flex-column justify-content-between h-100 m-3">
    <div class="row custom-row-cols g-2">
      <div class="col">
        <div class="row timer-wrapper" v-show="counting[0]">
          <div
            class="col d-flex timer-pic-wrapper justify-content-center align-items-center"
          >
            <Vue3Lottie
              ref="timer1"
              :animationData="TimerImg"
              :width="100"
              :autoPlay="false"
            />
            <div class="timer">
              <span>{{ time[0] }}</span>
            </div>
          </div>
        </div>
        <div class="row timer-wrapper" v-show="!counting[0]">
          <div
            class="d-flex flex-row justify-content-center align-items-center fw-bold fs-5"
          >
            30초
          </div>
        </div>
      </div>
      <div class="col">
        <div class="row timer-wrapper" v-show="counting[1]">
          <div
            class="col d-flex timer-pic-wrapper flex-row justify-content-center align-items-center"
          >
            <Vue3Lottie
              ref="timer2"
              :animationData="TimerImg"
              :width="100"
              :autoPlay="false"
            />
            <div class="timer">
              <span>{{ time[1] }}</span>
            </div>
          </div>
        </div>
        <div class="row timer-wrapper" v-show="!counting[1]">
          <div
            class="d-flex flex-row justify-content-center align-items-center fw-bold fs-5"
          >
            60초
          </div>
        </div>
      </div>
      <div class="col">
        <div class="row timer-wrapper" v-show="counting[2]">
          <div
            class="timer-pic-wrapper col d-flex justify-content-center align-items-center"
          >
            <Vue3Lottie
              ref="timer3"
              :animationData="TimerImg"
              :width="100"
              :autoPlay="false"
            />
            <div class="timer">
              <span>{{ time[2] }}</span>
            </div>
          </div>
        </div>
        <div class="row timer-wrapper" v-show="!counting[2]">
          <div
            class="d-flex flex-row justify-content-center align-items-center fw-bold fs-5"
          >
            90초
          </div>
        </div>
      </div>
    </div>
    <div class="d-flex flex-row justify-content-center">
      <div class="waiting-participant start-button w-50">
        <el-button round style="margin: 0 auto;" type="text" @click="startCountdown">
          <span class="fs-5 fw-bold">START</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import Vue3Lottie from "vue3-lottie";
import StartImg from "@/assets/lottie_json/Start.json";
import TimerImg from "@/assets/lottie_json/Timer.json";

const TEN_MINUTES = 60 * 10;
const second30 = 30;
export default {
  name: "Timer",
  components: {
    Vue3Lottie,
  },
  filters: {
    minutesAndSeconds(value) {
      var minutes = Math.floor(parseInt(value, 10) / 60);
      var seconds = parseInt(value, 10) - minutes * 60;
      return `${minutes}:${seconds}`;
    },
  },
  mounted() {
    for (var i = 0; i < this.time.length; i++) {
      this.time[i] = 0;
      this.counting[i] = false;
    }
    setInterval(() => {
      if (this.counting[0] || this.counting[1] || this.counting[2]) {
        for (var i = 0; i < this.time.length; i++) {
          this.time[i]--;
          if (this.time[i] < 0) {
            this.counting[i] = false;
            this.time[i] = 0;
          }
        }
      }
    }, 1000);
  },
  data() {
    return {
      TimerImg,
      StartImg,
      timer: TEN_MINUTES,
      counting: [false, false, false],
      time: [second30, second30 * 2, second30 * 3],
      timerName: ["timer1", "timer2", "timer3"],
    };
  },
  methods: {
    startCountdown: function () {
      for (var i = 0; i < this.time.length; i++) {
        console.log(this.time.length);
        this.counting[i] = false;
      }
      if (
        this.$refs["timer1"] != undefined &&
        this.$refs["timer2"] != undefined &&
        this.$refs["timer3"] != undefined
      ) {
        this.$refs["timer3"].stop();
        this.$refs["timer2"].stop();
        this.$refs["timer1"].stop();
      }
      for (var i = 0; i < this.time.length; i++) {
        this.time[i] = second30 * (i + 1);
        this.counting[i] = true;
      }
      if (
        this.$refs["timer1"] != undefined &&
        this.$refs["timer2"] != undefined &&
        this.$refs["timer3"] != undefined
      ) {
        this.$refs["timer3"].play();
        this.$refs["timer2"].play();
        this.$refs["timer1"].play();
      }
    },
  },
};
</script>

<style scoped>
.timer {
  height: 100%;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  font-size: calc(1.275rem + 0.3vw);
  font-weight: 700;
  transform: translateX(-80%);
}

.timer-wrapper {
  margin: 10px;
  border-radius: 20px;
  width: 50%;
  margin: 0 auto;
  height: 100px;
  box-shadow: inset 3px 3px 8px 0 rgba(0, 0, 0, 0.2),
              inset -6px -6px 10px 0 rgba(255, 255, 255, 0.5);
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}

.custom-row-cols > * {
  flex: 0 0 auto;
  width: 100%;
}

.timer-pic-wrapper {
  flex-direction: row;
}

@media screen and (max-width: 600px) {
  .timer {
    transform: translateX(0%);
  }
  .timer-wrapper {
    width: 80%;
    margin-top: 50px;
  }
  .custom-row-cols > * {
    flex: 0 0 auto;
    width: 33.3333333333%;
  }
  .timer-pic-wrapper {
    flex-direction: row;
  }
  .start-button {
    padding: 5px;
  }
}
</style>
