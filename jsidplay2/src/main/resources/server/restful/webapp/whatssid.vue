<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8" />
<title>WhatsSID</title>
</head>
<body>
	<!--script src="https://unpkg.com/vue"></script-->
	<script src="https://cdn.jsdelivr.net/npm/vue@2.6.0"></script>
	<script src="https://unpkg.com/axios/dist/axios.min.js"></script>

	<h1>WhatsSID?</h1>
	<span>Please use username='jsidplay2', password='jsidplay2!'</span>

	<div id="app">
		<h1>Upload a WAV File</h1>
		<form enctype="multipart/form-data">
			<input type="file" name="file"
				v-on:change="fileChange($event.target.files)" />
			<button type="button" v-on:click="upload()">Upload</button>
		</form>
		<div>
			<p>{{ match }}</p>
		</div>
	</div>

	<script>

	new Vue({
        el: '#app',
        data() {
            return {
            	match: '',
                files: new FormData()
            }
        },
        methods: {
            fileChange(fileList) {
                this.files = new FormData();
                this.files.append("file", fileList[0], fileList[0].name);
            },
            upload() {
            	this.match = 'Please wait...';
                axios({ method: "POST", "url": "/jsidplay2service/JSIDPlay2REST/whatssid", "data": this.files }).then(result => {
                	if (result.data && result.headers['content-length']) {
	                	this.match = result.data;
	               	} else {
	               		this.match = 'Sorry, no match!';
	               	}
                }, error => {
                    console.error(error);
                });
            }
        }
	})
</script>
</body>
</html>