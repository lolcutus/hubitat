library (
	base: "driver",
	author: "lolcutus",
	category: "driverUtilities",
	description: "aqaraUtilities",
	name: "aqaraUtilities",
	namespace: "lolcutus",
	documentationLink: "",
	importUrl: "https://raw.githubusercontent.com/lolcutus/hubitat/master/library/aqaraUtilities.groovy"
)

Map parseDescription(String description) {
	Map msgMap = null
	if(description.indexOf('encoding: 4C') >= 0) {
		msgMap = zigbee.parseDescriptionAsMap(description.replace('encoding: 4C', 'encoding: F2'))
		msgMap = unpackStructInMap(msgMap)
	} else if(description.indexOf('attrId: FF01, encoding: 42') >= 0) {
		msgMap = zigbee.parseDescriptionAsMap(description.replace('encoding: 42', 'encoding: F2'))
		msgMap["encoding"] = "41"
	} else {
		msgMap = zigbee.parseDescriptionAsMap(description)
	}
	msgMap
}
 
Map parseBattery(value) {
	def batteryVoltajeFirstIndex = 8
	def batteryVoltajeSecondIndex = 6
    def minVolts = 3
	def maxVolts = 3.1
	def model = getDataValue("model");
	switch(model){
		case "lumi.remote.b1acn01":
		case "lumi.remote.b186acn01":
		case "lumi.remote.b286acn01":
		case "lumi.sensor_motion.aq2":
            minVolts = 3
	        maxVolts = 3.33
			break
		case "lumi.sensor_magnet.aq2":
			minVolts = 2.9
	        maxVolts = 3.2
			break
	}
    debugLog("batteryVoltajeFirstIndex: " + batteryVoltajeFirstIndex)
	def batteryVoltaje = value[batteryVoltajeFirstIndex .. (batteryVoltajeFirstIndex+1)] + value[batteryVoltajeSecondIndex .. (batteryVoltajeSecondIndex+1)]
	debugLog("batteryVoltaje: " + batteryVoltaje)
	def rawVolts = Integer.parseInt(batteryVoltaje,16)/1000
	debugLog("rawVolts: " + rawVolts)
	
	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))
    if(roundedPct < 0) {
         roundedPct = 0       
    }
	def descText = "Battery level is ${roundedPct}% (${rawVolts} Volts)"
	
	def map = [:]
	map.name = "battery"
	map.value= roundedPct
	map.unit = "%"
	map.descriptionText = descText
	map
}
  