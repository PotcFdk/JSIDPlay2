<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8" />
<title>Favorites</title>
</head>
<body>
	<!--script src="https://unpkg.com/vue"></script-->
	<script src="https://cdn.jsdelivr.net/npm/vue@2.6.0"></script>
	<script src="https://unpkg.com/axios/dist/axios.min.js"></script>

	<div id="app">
		<h1>My favorite tunes</h1>
		<span>Please use username='jsidplay2', password='jsidplay2!'</span>
		<ul>
			<li v-for="favorite in favorites"><a
				v-bind:href="'/jsidplay2service/JSIDPlay2REST/convert' + favorite + '?defaultLength=03:00&enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel=MOS8580&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&cbr=64&vbrQuality=0&vbr=true'">
					{{favorite}} </a></li>
		</ul>
	</div>

	<script>
	new Vue({
		el: '#app',
		data: {
			favorites: ''
		},
		created: function () {
			this.fetchData();
		},        
		methods: {
			fetchData: function () {
				axios({
					method: 'get',
					url: '/jsidplay2service/JSIDPlay2REST/favorites'
				}).then(response => {
					this.favorites = response.data
				})
			}
		}
	})
</script>
</body>
</html>
