(ns clojars.queue
  (:require [durable-queue :as dq]
            [clojure.spec.alpha :as s]
            [io.pedestal.log :as log]))

(defprotocol Queueing
  (enqueue! [_ queue msg])
  (register-handler [_ queue f]
    "Provide a handler function. f takes one argument, the queue payload")
  (remove-handler [_ queue])
  (stats [_]))

(defn handler-loop
  [reporter queues queue f run?]
  (future
    (try
      (let [message-failures (atom {})]                      ;; map of id : num-attempts
        (while (get @run? queue)
          (let [task (dq/take! queues queue 100 ::timeout)]
            (when (not= ::timeout task)
              (try
                (f @task)
                (dq/complete! task)
                (catch Throwable e
                  (let [task-id (:id @task)
                        num-failures (swap! message-failures update task-id (fnil inc 0))]
                    (try (log/error :message "Exception thrown while processing handler loop" :exception e)
                         (reporter {:message "Exception thrown while processing handler loop"
                                                       :tags {"queue" queue}
                                                       :extra {:task @task
                                                               :num-failures num-failures}
                                                       :throwable e})
                         (if (<= (get num-failures task-id) 3)
                           (dq/retry! task)
                           (do
                             (log/error :message "Hit max failures, aborting task"
                                        :id task-id
                                        :num-failures num-failures)
                             ;; Clean up atom once task has been aborted.
                             (swap! message-failures dissoc task-id)
                             (dq/complete! task)))
                         (catch Throwable e
                           ;; report-error has a catch block already to catch if it throws while trying to report
                           ;; we add another catch block here because the call to report-error can also throw if
                           ;; your spec is incorrect (!).
                           (log/error :exception e :message "Exception while trying to report exception in queue handler"
                                      :queue queue)
                           (dq/complete! task))))))))))
      (catch Throwable e
        (reporter e)
        ;; TODO: limit # of restarts?
        (handler-loop reporter queues queue f run?)))))

#_(defrecord DurableQueueing [queues run-state error-reporter]
  Queueing
  (enqueue! [_ queue msg]
    (dq/put! queues queue msg))
  (remove-handler [_ queue]
    (swap! run-state dissoc queue))
  (register-handler [t queue f]
    (remove-handler t queue)
    (swap! run-state assoc queue true)
    (handler-loop error-reporter queues queue f run-state))
  (stats [_]
    (dq/stats queues))

  component/Lifecycle
  (start [t] t)
  (stop [t]
    (reset! run-state nil)
    t))

#_(defn queue-component [slab-dir]
  ;; fsync on every take to reduce chance of redoing a task on restart
  ;; our throughput needs are meager
  (map->DurableQueueing {:queues (dq/queues slab-dir {:fsync-take? true})
                         :run-state (atom nil)}))

(s/def :clojars.queue/queuing #(satisfies? Queueing %))
