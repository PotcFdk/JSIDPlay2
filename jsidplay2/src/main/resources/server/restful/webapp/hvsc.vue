<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8" />
<title>HVSC</title>
</head>
<body>
	<!--script src="https://unpkg.com/vue"></script-->
	<script src="https://cdn.jsdelivr.net/npm/vue@2.6.0"></script>
	<script src="https://unpkg.com/axios/dist/axios.min.js"></script>

	<h1>HVSC</h1>
	<span>Please use username='jsidplay2', password='jsidplay2!'</span>

	<div id="app">
		<ul>
			<li v-for="entry in directory" :key="entry">
				<!-- HVSC root -->
				<div v-if="entry.endsWith('/')">
					<a href="#"
						v-on:click="fetchData('directory', entry.substring(0, entry.length-1))">
						{{entry}} </a>
				</div> <!-- HVSC music -->
				<div
					v-else-if="entry.endsWith('.sid') || entry.endsWith('.dat') || entry.endsWith('.mus') || entry.endsWith('.str')">
					<div>
						<a
							v-bind:href="'/jsidplay2service/JSIDPlay2REST/convert' + encodeURI(entry).replace(/\+/g,'%2B') + '?defaultLength=03:00&enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel=MOS8580&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true&cbr=64&vbrQuality=0&vbr=true'"
							target="_blank"> {{entry}} </a>
					</div>
					<div>
						<img
							v-bind:src="'/jsidplay2service/JSIDPlay2REST/photo' + encodeURI(entry).replace(/\+/g,'%2B')" />
					</div>
				</div> <!-- others -->
				<div v-else>
					<div>
						<a
							v-bind:href="'/jsidplay2service/JSIDPlay2REST/convert' + encodeURI(entry).replace(/\+/g,'%2B') + '?startTime=01:15&defaultLength=05:00&enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel=MOS8580&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true'"
							target="_blank"> {{entry}} </a>
					</div>
				</div>
			</li>
		</ul>
	</div>

	<script>
	new Vue({
		el: '#app',
		data: {
			directory: '',
			imgData: []
		},
		created: function () {
			this.fetchData('directory', '/');
		},        
		methods: {
			fetchData: function (type, entry) {
				if (type == 'directory') {
					axios({
						method: 'get',
						url: '/jsidplay2service/JSIDPlay2REST/' + type + encodeURI(entry).replace(/\+/g,'%2B') + '?filter=.*%5C.(sid%7Cdat%7Cmus%7Cstr%7Cmp3%7Cmp4%7Cjpg%7Cprg%7Cd64)$'
					}).then(response => {
						this.directory= response.data;
					})
				}
			}
		}
	})
</script>
</body>
</html>


