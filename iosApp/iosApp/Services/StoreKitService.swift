import Foundation
import StoreKit

/// StoreKit 2 facade for Birdo plan purchases.
///
/// Product-id mapping is `app.birdo.vpn.<plan>.<period>`, e.g.
/// `app.birdo.vpn.spectre.monthly`. These ids must match the
/// auto-renewable subscription product ids configured in App Store Connect.
@MainActor
final class StoreKitService: ObservableObject {
    static let shared = StoreKitService()

    @Published private(set) var products: [String: Product] = [:]
    @Published private(set) var purchasedProductIDs: Set<String> = []
    @Published private(set) var isLoading = false
    @Published var lastError: String?

    private var transactionListener: Task<Void, Never>?

    init() {
        // Listen for Ask-To-Buy approvals, refunds, family-sharing changes,
        // restore-from-backup transactions, etc. across the entire app
        // lifetime. Apple's docs require this listener at launch.
        transactionListener = Task.detached { [weak self] in
            for await update in Transaction.updates {
                await self?.handle(transactionResult: update)
            }
        }
    }

    deinit {
        transactionListener?.cancel()
    }

    // MARK: - Catalog

    /// Fetch product metadata for every plan × billing-period combination.
    /// Safe to call multiple times — cached after first success.
    func loadProducts() async {
        guard products.isEmpty else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            let ids = Self.allProductIDs
            let fetched = try await Product.products(for: ids)
            for product in fetched {
                products[product.id] = product
            }
            await refreshEntitlements()
        } catch {
            lastError = error.localizedDescription
        }
    }

    // MARK: - Purchase

    /// Kick off the StoreKit 2 purchase sheet for the given product id.
    /// Returns `true` on a verified, finished transaction.
    @discardableResult
    func purchase(productID: String) async -> Bool {
        await loadProducts()
        guard let product = products[productID] else {
            lastError = "Plan unavailable on this device."
            return false
        }
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                let transaction = try checkVerified(verification)
                purchasedProductIDs.insert(transaction.productID)
                await transaction.finish()
                return true
            case .userCancelled:
                return false
            case .pending:
                // Ask-to-Buy / SCA — user will be notified asynchronously.
                return false
            @unknown default:
                return false
            }
        } catch {
            lastError = error.localizedDescription
            return false
        }
    }

    /// Re-check `Transaction.currentEntitlements` and update the active set.
    /// Called on launch and after every successful purchase / restore.
    func refreshEntitlements() async {
        var active: Set<String> = []
        for await result in Transaction.currentEntitlements {
            guard case .verified(let transaction) = result,
                  transaction.revocationDate == nil,
                  !(transaction.isUpgraded) else { continue }
            // Active subscription if it has no expiry or expires in the future.
            if let expiration = transaction.expirationDate, expiration < .now {
                continue
            }
            active.insert(transaction.productID)
        }
        purchasedProductIDs = active
    }

    /// Re-fetch transactions from the App Store (Restore Purchases button).
    func restorePurchases() async {
        do {
            try await AppStore.sync()
            await refreshEntitlements()
        } catch {
            lastError = error.localizedDescription
        }
    }

    // MARK: - Helpers

    private func handle(transactionResult: VerificationResult<Transaction>) async {
        do {
            let transaction = try checkVerified(transactionResult)
            purchasedProductIDs.insert(transaction.productID)
            await transaction.finish()
        } catch {
            lastError = error.localizedDescription
        }
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified(_, let error):
            throw error
        case .verified(let safe):
            return safe
        }
    }

    // MARK: - Catalog Constants

    /// Product-id catalog. Order matches `Plan` × `BillingPeriod`.
    static let allProductIDs: Set<String> = [
        "app.birdo.vpn.recon.monthly",   "app.birdo.vpn.recon.yearly",
        "app.birdo.vpn.spectre.monthly", "app.birdo.vpn.spectre.yearly",
        "app.birdo.vpn.phantom.monthly", "app.birdo.vpn.phantom.yearly",
        "app.birdo.vpn.eagle.monthly",   "app.birdo.vpn.eagle.yearly",
    ]

    /// Compose a product id from the plan slug + billing period suffix.
    static func productID(planSlug: String, isYearly: Bool) -> String {
        "app.birdo.vpn.\(planSlug.lowercased()).\(isYearly ? "yearly" : "monthly")"
    }
}
