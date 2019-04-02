var express = require("express");
var bodyParser = require('body-parser');
var sender = require('request');
var axios = require('axios');
var log4js = require('log4js');

var logger = log4js.getLogger();
logger.level = 'debug';

/* end of dependency setup */

var port = process.env.PORT || 8080;

var app = express();

app.use(function(req, res, next) {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept');
  next();
});

app.post('/process_js', function(req, res) {

    var traceid = req.headers["x-b3-traceid"];
    var spanid = req.headers["x-b3-spanid"];
    var sampled = req.headers["x-b3-sampled"];
    var parent = req.headers["x-b3-parentspanid"];
    
    if ( traceid ) {
	console.log("Got traceid: " + traceid);
    }

    if ( spanid ) {
	console.log("Got spanid: " + spanid);
    }

    if ( sampled ) {
	console.log("Got sampled: " + sampled);
    }

    if ( parent ) {
	console.log("Got parent id: " + parent);
    }
    
    console.log('Running JS pipeline process');

    var path  = process.env.NEXT_STEP_URL;

    axios.defaults.headers.post["x-b3-traceid"] = traceid;
    axios.defaults.headers.post["x-b3-parentspanid"] = parent;
    axios.defaults.headers.post["x-b3-sampled"] = sampled;
    axios.defaults.headers.post["x-b3-spanid"] = spanid;

    axios.post(path,{}).then((response) => {
	logger.debug("Finished pipeline call");

	console.log(response.data);
	console.log(response.status);
	console.log(response.statusText);
	console.log(response.headers);
	console.log(response.config);

	
	
	res.send(JSON.stringify({
	    outcome: "success"
	}, null, 3));
    }).catch(err => {console.log(err); res.send(); });
});


app.listen(port);
logger.debug("Listening on port ", port);
