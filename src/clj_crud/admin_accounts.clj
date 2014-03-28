(ns clj-crud.admin-accounts
  (:require [clojure.tools.logging :refer [debug spy]]
            [clj-crud.system.email :as email]
            [clj-crud.util.layout :as l]
            [clj-crud.util.helpers :as h]
            [clj-crud.common :as c]
            [clj-crud.data.accounts :as accounts]
            [liberator.core :refer [resource defresource]]
            [liberator.representation :as lib-rep]
            [compojure.core :refer [defroutes ANY GET context]]
            [net.cgrand.enlive-html :as html]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]))

(def admin-users-list-html (html/html-resource "templates/admin/users.html"))

(defn admin-users-list-layout [ctx]
  (c/emit-application
   ctx
   [:#content] (html/content admin-users-list-html)
   [:a.rel-home] (html/set-attr :href (get-in ctx [:data :links :home :uri]))
   [:a.rel-users] (let [{:keys [rel uri] :as users} (get-in ctx [:data :links :users])]
                    (html/do->
                     (html/content rel)
                     (html/set-attr :href uri)))
   [:a.rel-new-user] (html/set-attr :href (get-in ctx [:data :links :new :uri]))
   [:table#users :tbody [:tr html/first-of-type]]
   (html/clone-for [user (get-in ctx [:data :users])]
                   [:tr] (let [{:keys [id slug name created_at updated_at]} user
                               edit-link (get-in user [:links :edit :uri])
                               self-link (get-in user [:links :self :uri])]
                           (html/transform-content
                            [:td.id] (html/content (str id))
                            [:td.slug :a] (html/do->
                                           (html/content slug)
                                           (html/set-attr :href self-link))
                            [:td.name :a] (html/do->
                                           (html/content name)
                                           (html/set-attr :href self-link))
                            [:td.created_at] (html/content (str created_at))
                            [:td.updated_at] (html/content (str updated_at))
                            [:td.edit :a] (html/set-attr :href edit-link)
                            )))))

(defn with-user-links [{:keys [slug] :as user}]
  (assoc user :links {:self {:rel "self"
                             :uri (str "/profile/" slug)}
                      :edit {:rel "edit"
                             :uri (str "/profile/" slug "/edit")}
                      :delete {:rel "delete"
                               :uri (str "/profile/" slug "/delete")}}))

(def admin-index-html (html/html-resource "templates/admin/index.html"))

(defn admin-index-layout [ctx]
  (c/emit-application
   ctx
   [:#content] (html/content admin-index-html)
   [:a.rel-home] (html/set-attr :href (get-in ctx [:data :links :home :uri]))
   [:a.rel-accounts] (let [uri (get-in ctx [:data :links :accounts :uri])]
                       (html/set-attr :href uri))))

(defresource admin-index
  :available-media-types ["text/html" "application/edn"]
  :authorized? (fn [ctx]
                 (spy (friend/identity (get ctx :request))))
  :handle-unauthorized (fn [ctx]
                         (h/location-flash "/login"
                                           "Please login"))
  :allowed? (fn [ctx]
                 (let [slug (get-in ctx [:request :params :slug])]
                   (friend/authorized? [:admin] (friend/identity (get ctx :request)))))
  :handle-forbidden (fn [ctx]
                      (h/location-flash "/login"
                                        "Not allowed"))
  :handle-ok {:main "Hello admin world!"
              :links {:home {:uri "/admin"
                             :rel "home"}
                      :users {:uri "/admin/accounts"
                              :rel "accounts"}}}
  :as-response (l/as-template-response admin-index-layout))


(defroutes admin-accounts-routes
  (context "/admin" _
           (ANY "/" _ admin-index)
           #_(ANY "/accounts" _ admin-accounts-list)))
