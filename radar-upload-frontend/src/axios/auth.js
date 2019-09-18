import axios from 'axios';

export default {
  logout() {
    return axios.get('/logout');
  },
  async login() {
    const { authorizationBearer } = await axios.get('/login');
    document.cookie = `authorizationBearer=${authorizationBearer}`;
  },
};
