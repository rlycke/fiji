#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

# Fetch all the samples into a local directory

from fiji import User_Plugins

from ij import IJ, ImageJ, Menus

from java.lang import System

from os import makedirs, rename

from os.path import exists, isdir

import urllib

ij = ImageJ()

menu = User_Plugins.getMenuItem('File>Open Samples')
commands = Menus.getCommands()
plugin = 'ij.plugin.URLOpener("'
samples = System.getProperty('fiji.dir') + '/samples'

for i in range(0, menu.getItemCount()):
	label = menu.getItem(i).getLabel()
	if label == '-':
		continue
	command = commands[label]
	if command != None and \
			command.startswith(plugin) and command.endswith('")'):
		url = command[len(plugin):-2]
		slash = url.rfind('/')
		if slash < 0:
			name = url
			url = IJ.URL + '/images/' + url
		else:
			name = url[slash + 1:]

		target = samples + '/' + name
		if exists(target):
			print 'Already have', name
			continue

		print 'Download', name
		filename = urllib.urlretrieve(url)[0]
		if not isdir(samples):
			makedirs(samples)
		rename(filename, target)
	else:
		print 'Skipping unknown command', command, 'for label', label

print 'Done'
ij.dispose()
