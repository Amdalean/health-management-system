import { createStore } from "vuex";
import user from '@/store/modules/user'
import theme from '@/store/modules/theme'
import getters from './getters'

const store = createStore({
  modules: {
    user,
    theme
  },
  getters
});
export default store;



