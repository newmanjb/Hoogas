# Hoogas

--This is very much in alpha at the moment but the premise is below--

If your being honest with yourself and you decide, that even though it would be pretty cool, your system doesn't need to be able to handle 999,000 msgs per nano-second that can be replicated over 5 queues in all 7 continents, and that you don't have to jack into the Matrix in order to monitor and manage your applications, but that you DO want to be able to ensure your log files are consistent, your directory structures between environments match up, global configuration settings are applied consistently and can be dynamically updated, that you can easily port all or part of your system to developer's machines, you can easily record production data to use as test data, and you don't want to spend months setting all this up using different frameworks who's functionality you'll only ever use about 30% of, then maybe Hoogas is for you.

Hoogas is a management application for large systems that takes care of the fiddly work of achieving consistency for non-functional aspects such logging, global configuration, archiving, dynamic config updates, starting and stopping applications, monitoring them, setting them up in different environment with test data, and a lot more, and all without using many competing frameworks with steep learning curves, large memory footprints and confusing documentation.

It's almost 100% core java and has very few dependencies.  It's very lightweight and very non-intrusive.  It's for non-functional requirements only, so it doesn't interfere with anyone's creativity or style.  
