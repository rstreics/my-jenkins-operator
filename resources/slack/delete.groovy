package slack

import jenkins.model.Jenkins
import net.sf.json.JSONObject

def slackParameters = [:]

def jenk = Jenkins.get()
def slack = jenk.getExtensionList(jenkins.plugins.slack.SlackNotifier.DescriptorImpl)[0]

def formData = ['slack': ['tokenCredentialId': 'slack-token']] as JSONObject
def request = [getParameter: { name -> slackParameters[name] }] as org.kohsuke.stapler.StaplerRequest
slack.configure(request, formData)

// save to disk
slack.save()
jenk.save()

println 'Status: CONVERGED <EOF>'
