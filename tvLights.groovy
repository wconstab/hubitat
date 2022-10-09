definition(
	name: "TV Lights",
	namespace: "hubitat",
	author: "Will Constable",
	description: "Dim the lights for TV watching, and then restore them to their previous levels",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
	page(name: "mainPage")
}


Map mainPage() {
	dynamicPage(name: "mainPage", title: "TV Lights", uninstall: true, install: true) {
		section {
            input "theDenon", "capability.switch", title: "The AVR to monitor for on/off", multiple: false
            input "theLights", "capability.switch", title: "Lights to control", multiple: true
			input "nanoMote", "capability.pushableButton", title: "NanoMote used for arm/disarm and trigger", multiple: false
            //input "disarmButton", "capability.pushableButton", title: "Disarm Button", multiple: false
            input "armedSignal", "capability.colorControl", title: "Armed Signal Color LED"
            
            input name:"update", type:"button", title:"Update"

		}
	}
}

/*
TODO

- figure out a way to only react to the TV modes of denon being on, instead of its sw update
- get arm/disarm working from innovelli clicks
- clean up logging
- consider a delay for turning off lights, during delay period user can cancel via remote click
- set up dimming of lights instead of turning them off
    - need a UI for specifying multiple lights and their respective dim levels
    - stopgap just hardcode them? not even sure how to hardcode a device
*/


void updated() {
    log.debug "updated()"
    unsubscribe()
	  initialize()

    // for debugging
    //captureLights()
    //restoreLights()

}

void installed() {
	  initialize()
}

void initialize() {
	subscribe(theDenon, "switch.off", denonHandler)
    subscribe(theDenon, "switch.on", denonHandler)
    subscribe(nanoMote, "pushed", nanoMotePushedHandler)
    subscribe(nanoMote, "held", nanoMoteHeldHandler)
	subscribe(switches, "switch.on", onHandler)
	atomicState.someOn = true

}

def appButtonHandler(update) {
    updated()
}

/*  NanoMote Guide
1 - moon
2 - people
3 - center
4 - power
*/

void nanoMotePushedHandler(evt) {
    log.debug "nanoMotePushedHandler() called: ${evt.name} ${evt.value}"
    if(evt.value == "1") {
        if(atomicState.armed) {
            log.debug "nanoMotePushedHandler triggering TV lights"
            captureLights()
            dimLights()
        }
    }
}

void nanoMoteHeldHandler(evt) {
    log.debug "nanoMoteHeldHandler() called: ${evt.name} ${evt.value}"
    if(evt.value == "1") {
        if(atomicState.armed) {
            log.debug "nanoMote disarming TV lights"
            armedSignal.setColor(hue: 0, saturation: 100)
            atomicState.armed = false
        } else {
            log.debug "nanoMote arming TV lights"
            armedSignal.setColor(hue: 33, saturation:100)
            atomicState.armed = true            
        }
    }
}

void denonHandler(evt) {
    log.debug "handler() called: ${evt.name} ${evt.value}"
    if(evt.value == "on") {
        log.debug "Denon turned on!"
        //if(atomicState.armed) {
        //    log.debug "Denon triggered dimLights()"
        //    captureLights()
        //    dimLights()
        //}
    } else {
        log.debug "Denon turned off!"
        if(atomicState.armed) {
            restoreLights()
        }
    }
}

void captureLights() {
    atomicState.lightState = []
    theLights.eachWithIndex { it, idx ->
        log.debug "Capture: $it - $it.currentSwitch - $it.currentLevel"
        atomicState.lightState += [idx: idx, state: it.currentSwitch, level:it.currentLevel]
    }
    log.debug "Captured $atomicState.lightState.size lights"
}

void dimLights() {
    theLights.each {
        it.off()
    }
}

void restoreLights() {
    log.debug("restoreLights")
    atomicState.lightState.each{
        if(it.state == "on") {
            log.debug "restoring turned on light $it.idx to level $it.level"
            theLights[it.idx].on()
            theLights[it.idx].setLevel(it.level)
        }
    }
}
